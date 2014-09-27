#!/usr/bin/perl

# use modules
use File::Slurp;
use XML::Simple;
# use Data::Dumper;

# output Unicode
binmode(STDOUT, ":utf8");

# create xml object
$xml = new XML::Simple;

# create and read xml file - this should point to KlingonAssistant's data path
system('cd ../KlingonAssistant/data/ && ./generate_db.sh --xmlonly');
$sm_export = read_file('../KlingonAssistant/data/mem.xml');

# do substitutions to make it ready for conversion
$sm_export =~ s/\s*<!--.*?-->//sg;
$sm_export =~ s/<table name="mem">(.*?)<\/table>/<mem>\1<\/mem>/sg;
$sm_export =~ s/<column name="(.*?)">(.*?)<\/column>/<\1>\2<\/\1>/sg;

# convert processed xml file to xml
$data = $xml->XMLin($sm_export, suppressempty => '');

# keep track of syllables
my %syllables;

# Now we have all the data in an xml object. Cycle through and process.
foreach my $e (@{$data->{database}->{mem}})
{
  (my $condensed = $e->{entry_name}) =~ s/[^A-Za-z'\-]+//;
  $condensed =~ s/-$/0/g;
  $condensed =~ s/[\-\.\?!,;]//g;
  # remove the "X" placeholder from sentences.
  $condensed =~ s/X//g;
  $condensed =~ s/^ //g;
  $condensed =~ s/  / /g;
  # {gh} has to be done before {ng} so that {ngh} -> "nG" and not "Fh".
  # {gh} has to be replaced with "G" at first to distinguish "nG" from "ng".
  $condensed =~ s/ch/C/g;
  $condensed =~ s/gh/G/g;
  $condensed =~ s/ng/F/g;
  $condensed =~ s/tlh/x/g;
  $condensed =~ s/'/z/g;
  $condensed =~ s/Q/k/g;
  $condensed = lc $condensed;

  # keep stripping syllables off the end
  while ($condensed =~ /(.*)([^aeiou][aeiou][^aeiou]*)/) {
    $s = $2;
    $condensed = $1;
    $syllables{$s} = $e->{entry_name};
    if ($condensed =~ /.*[aeiou]$/) {
      # if ends in vowel, treat next syllable as half syllable
      $condensed = $condensed.'0';
    } elsif ($condensed =~ /.* $/) {
      # skip spaces, this must happen after the vowel check
      chop $condensed;
    }
  }
  if (length $condensed) {
    $syllables{$condensed} = $condensed;
  }
}

my %front_half_syllables;
my %short_syllables;
my %full_syllables;

for my $s (sort keys %syllables) {
  my $t = $s;
  $t =~ s/0$/-/g;
  $t =~ s/c/C/g;
  $t =~ s/d/D/g;
  $t =~ s/g/G/g;
  $t =~ s/h/H/g;
  $t =~ s/i/I/g;
  $t =~ s/f/F/g;
  $t =~ s/k/Q/g;
  $t =~ s/s/S/g;
  if (! -e '../KlingonTtsEngine/res/raw/'.$s.'.mp3') {
    print "Warning: File ", $s, ".mp3 not found! Required for: {", $syllables{$s}, "}\n";
  } else {
    if (length $s == 2) {
      $short_syllables{$s} = $t;
    } elsif (substr($s,2) eq '0') {
      $front_half_syllables{$s} = $t;
    } else {
      $full_syllables{$s} = $t;
    }
  }
}

# Generate code in the Java file to produce the audio map.
$java_file_name = '../KlingonTtsEngine/src/org/tlhInganHol/android/klingonttsengine/KlingonSpeakTtsService.java';
$java_file = read_file($java_file_name);

$front_half_code = "";
for my $s (sort keys %front_half_syllables) {
  my $line = sprintf("        initMap.put(\"%s\", R.raw.%s);\n", $front_half_syllables{$s}, $s);
  $front_half_code = $front_half_code . $line;
}
unless ( $java_file =~ s/(BEGIN: FRONT_HALF_SYLLABLE_TO_AUDIO_MAP\n).*(^\s+\/\/ END: FRONT_HALF_SYLLABLE_TO_AUDIO_MAP)/$1$front_half_code$2/smg ) {
  print "ERROR: Failed to write FRONT_HALF_SYLLABLE_TO_AUDIO_MAP.\n";
  exit;
}

$short_syllables_code = "";
for my $s (sort keys %short_syllables) {
  my $line = sprintf("        initMap.put(\"%s\", R.raw.%s);\n", $short_syllables{$s}, $s);
}
unless ( $java_file =~ s/(BEGIN: SHORT_SYLLABLE_TO_AUDIO_MAP\n).*(^\s+\/\/ END: SHORT_SYLLABLE_TO_AUDIO_MAP)/$1$short_syllables_code$2/smg ) {
  print "ERROR: Failed to write SHORT_SYLLABLE_TO_AUDIO_MAP.\n";
  exit;
}

$full_syllables_code = "";
for my $s (sort keys %full_syllables) {
  my $line = sprintf("        initMap.put(\"%s\", R.raw.%s);\n", $full_syllables{$s}, $s);
}
unless ( $java_file =~ s/(BEGIN: MAIN_SYLLABLE_TO_AUDIO_MAP\n).*(^\s+\/\/ END: MAIN_SYLLABLE_TO_AUDIO_MAP)/$1$full_syllables_code$2/smg ) {
  print "ERROR: Failed to write MAIN_SYLLABLE_TO_AUDIO_MAP.\n";
  exit;
}

# Write to the Java file.
write_file $java_file_name, {binmode => ':utf8'}, $java_file;
