#!/usr/bin/perl

# use modules
use File::Slurp;
use XML::Simple;
# use Data::Dumper;

# output Unicode
binmode(STDOUT, ":utf8");

# create xml object
$xml = new XML::Simple;

# read xml file - this should point to KlingonAssistant's data path
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
  $condensed =~ s/ //g;
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
    $syllables{$2} = $e->{entry_name};
    $condensed = $1;
  }
  if (length $condensed) {
    $syllables{$condensed} = $condensed;
  }
}

for my $s (sort keys %syllables)
{
  if (! -e '../KlingonTtsEngine/res/raw/'.$s.'.mp3') {
    print "Warning: File ", $s, ".mp3 not found! Required for: {", $syllables{$s}, "}\n";
  }
}
