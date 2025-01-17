import sys
import re

OF = open("formatted_serotype_results.tsv","w")

F1 = open(sys.argv[1],"r")
h_type = "NIL"
o_type = "NIL"
n = 0
for i in F1:
    temp = i.split()
    if re.search("H_type", temp[0]):
        h_type = temp[2]
    if re.search("O_type",temp[0]):
        if n == 0:
            o_type = temp[2]
        else:
            if o_type == temp[2]:
                pass
            else:
                o_type = o_type + "/" + temp[2]
        n = n + 1
        #o_type = temp[2]

if h_type != "NIL" and o_type != "NIL":
    serotype = o_type + ":" + h_type
elif h_type != "NIL" and o_type == "NIL":
    serotype = o_type + ":" + "HNT"
elif h_type == "NIL" and o_type != "NIL":
    serotype = "ONT" + ":" + h_type
else: 
    serotype = "ONT" + ":" + "HNT"

F1.close()

OF.write("Serotype\n")
OF.write(serotype + "\n")


OF.close()
