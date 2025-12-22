# Test targets

# Use local mill wrapper if available, otherwise system mill
MILL := $(shell if [ -x ./mill ]; then echo ./mill; else echo mill; fi)

.PHONY: test test-only

# Run all tests
test:
	$(MILL) api.test

# Run specific test class
# Usage: make test-only T=ConfigTest
test-only:
	$(MILL) api.test.testOnly -- *$(T)*
