clean:
	rm -f site/content/resources/mastodon.csv site/content/members.adoc site/content/stats.adoc site/content/podcasts.adoc site/content/resources/java-champions.yml

prerequisites-check:
ifeq (, $(shell which jbang))
	$(error 'No JBang in PATH, you may install it with https://sdkman.io')
endif
ifeq (, $(shell which jbake))
	$(error 'No JBake in PATH, you may install it with https://sdkman.io')
endif
	@echo 'JBang and JBake installations found.'

build: prerequisites-check
	cd resources; jbang site.java ../ ../site/content/
	cp java-champions.yml site/content/resources
	cd site; jbake --bake

server: build
	cd site; jbake --server
