#!/usr/bin/perl

# use modules
use File::Slurp;
use XML::Simple;

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

# Now we have all the data in an xml object. Cycle through and process.
foreach $e (@{$data->{database}->{mem}})
{
  # First, save entries which are just full sentences.
  if ($e->{part_of_speech} =~ m/^sen:/) {
    print $e->{definition}, "=", $e->{entry_name}, "\n";
  }

  # Next, process verbs.
  if ($e->{part_of_speech} =~ m/^v:/) {

  }
}
