clean:
	rm site/content/resources/mastodon.csv site/content/members.adoc site/content/stats.adoc

prerequisites-check:
ifeq (, $(shell which jbang))
	$(error 'No JBang in PATH, you may install it with https://sdkman.io')
endif
ifeq (, $(shell which jbake))
	$(error 'No JBake in PATH, you may install it with https://sdkman.io')
endif
	@echo 'JBang and JBake installations found.'

build: prerequisites-check
	cd resources; jbang site.java ../java-champions.yml ../site/content/
	cd site; jbake --bake

server: build
	cd site; jbake --server
