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

# print Dumper($sm_export);

# Declare the output files.
my $generated_verbatim="generated/pairs/verbatim.txt";
my $GV;
open($GV, '>', $generated_verbatim);

# Now we have all the data in an xml object. Cycle through and process.
foreach my $e (@{$data->{database}->{mem}})
{
  # First, save entries which are just full sentences, except for certain special ones.
  if (($e->{part_of_speech} =~ m/^sen:/) && ($e->{definition} !~ m/\.\.\.|\(|\{/)) {
    print {$GV} $e->{entry_name}, " = ", $e->{definition}, "\n";
  }

  # Next, process verbs.
  if ($e->{part_of_speech} =~ m/^v:/) {
    print {$GV} $e->{entry_name}, " = v. ", $e->{definition}, "\n";
  }

  # Process nouns.
  if ($e->{part_of_speech} =~ m/^n:/) {
    print {$GV} $e->{entry_name}, " = n. ", $e->{definition}, "\n";
  }
}
close($GV);
