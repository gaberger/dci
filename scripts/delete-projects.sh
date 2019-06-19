for i in $(../target/dci.js project list --json | jq '.[] | select(.name|test("^test."))| .id' -r)
do
	../target/dci.js project delete $i --force
done
