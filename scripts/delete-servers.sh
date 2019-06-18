for i in $(../target/dci.js server list --json | jq '.[].id' -r)
do
	../target/dci.js server delete $i --force
done
