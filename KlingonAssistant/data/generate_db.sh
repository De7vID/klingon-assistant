#!/bin/bash

# Concatenate data into one file.
cat mem-00-header.xml mem-01-b.xml mem-02-ch.xml mem-03-D.xml mem-04-gh.xml mem-05-H.xml mem-06-j.xml mem-07-l.xml mem-08-m.xml mem-09-n.xml mem-10-ng.xml mem-11-p.xml mem-12-q.xml mem-13-Q.xml mem-14-r.xml mem-15-S.xml mem-16-t.xml mem-17-tlh.xml mem-18-v.xml mem-19-w.xml mem-20-y.xml mem-21-a.xml mem-22-e.xml mem-23-I.xml mem-24-o.xml mem-25-u.xml mem-26-footer.xml > mem.xml

# Convert from xml to sql instructions.
./xml2sql.pl > mem.sql

# Print any entries with duplicate columns.
grep "ARRAY" mem.sql

# Create db binary.
if [ -f ../assets/qawHaq.db ];
then
    # If the db already exists, show a diff.
    sqlite3 ../assets/qawHaq.db .dump > old-mem.sql
    vimdiff old-mem.sql mem.sql
    read -n1 -r -p "Press any key to generate new db..."
    echo
    mv ../assets/qawHaq.db ../assets/qawHaq.db~
fi
sqlite3 ../assets/qawHaq.db < mem.sql

# Sanity check.
sqlite3 ../assets/qawHaq.db .dump > sanity.sql
diff mem.sql sanity.sql

# Pause (in case of error).
read -n1 -r -p "Press any key to delete temporary files..."
echo

# Clean up temporary files.
rm mem.xml
rm mem.sql
rm sanity.sql
rm -f old-mem.sql
