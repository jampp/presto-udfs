all:
	git clone git@github.com:Lewuathe/docker-presto-cluster.git
	mv docker-presto-cluster/ temp-presto-cluster-files/
	cp docker-compose.template docker-compose.yml
	rm temp-presto-cluster-files/Makefile temp-presto-cluster-files/docker-compose.yml temp-presto-cluster-files/README.md
	mv temp-presto-cluster-files/* ./
	rm -rf temp-presto-cluster-files/
	docker-compose build

.PHONY: test clean

run:
	docker-compose up -d
	echo "Please check http://localhost:8080"

run-with-logs:
	docker-compose up

down:
	docker-compose down

clean:
	rm -rf presto/ presto-base/ presto-coordinator/ presto-worker/
	rm -f LICENSE
	rm -rf temp-presto-cluster-files
	rm docker-compose.yml
