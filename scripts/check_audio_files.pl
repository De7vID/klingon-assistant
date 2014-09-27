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
  # TODO convert to other form
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

print "\n        Front halves:\n";
for my $s (sort keys %front_half_syllables) {
  print "        initMap.put(\"", $s, "\", R.raw.", $front_half_syllables{$s}, ");\n";

}

print "\n        Short syllables:\n";
for my $s (sort keys %short_syllables) {
  print "        initMap.put(\"", $s, "\", R.raw.", $short_syllables{$s}, ");\n";
}

print "\n        Full syllables:\n";
for my $s (sort keys %full_syllables) {
  print "        initMap.put(\"", $s, "\", R.raw.", $full_syllables{$s}, ");\n";
}

