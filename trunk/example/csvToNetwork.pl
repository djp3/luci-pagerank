#!/usr/bin/perl -I/Users/djp3/perl/lib/perl5/site_perl -W

#This scripts takes in a csv file without a header row of the format
#"2470carol1","noakay",2
"a008580152","Crono009",5
"a008580152","football0808",4
"abcd","vtctester",2
"Aharburg","harburgm",1
"akirawong","gemusan",1
"akorman","akorman2",1
"akorman","mdnew",1

use strict;
use warnings;
use Text::CSV;

my $file = 'resultset.csv';

my $csv = Text::CSV->new();

open (CSV, "sort -f ".$file."|") or die $!;
open (OUT, "> resultset.out") or die $!;

print OUT "[";

my $i = 0;
my @data;
my %people;
my %source;
my $key;
my $personcount = 0;
my $currentperson = "";
while (<CSV>) {
	if ($csv->parse($_)) {
		my @columns = $csv->fields();
		chomp $columns[0];
		chomp $columns[1];
		chomp $columns[2];
		$columns[0] =~ s/ //g;
		$columns[1] =~ s/ //g;
		if(!defined($people{$columns[0]})){
			$people{$columns[0]} = (++$personcount);
		}
		if(!defined($source{$columns[0]})){
			$source{$columns[0]} = 1;
		}
		if(!defined($people{$columns[1]})){
			$people{$columns[1]} = (++$personcount);
		}
		if(!defined($source{$columns[1]})){
			$source{$columns[1]} = 0;
		}
		if($currentperson ne $columns[0]){
			if($currentperson eq ""){
				print OUT "\n\t";
			}
			else{
				print OUT "],\n\t";
			}
			$currentperson = $columns[0];
			$source{$columns[0]}=1;
			print OUT "[".$people{$columns[0]}.",\"".$currentperson."\",1.0,";
		}
		print OUT "[".$people{$columns[1]}.",".$columns[2]."],";
	} else {
		my $err = $csv->error_input;
		print "Failed to parse line: $err";
	}
}
print OUT "],\n\t";

for $key (sort (keys %people)){
	print $key,"\n";
	if($source{$key} == 0){
		print OUT "[".$people{$key}.",\"".$key."\",1.0],\n\t";
	}
}

print OUT "\n]";
close OUT;
