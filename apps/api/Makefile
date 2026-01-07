# Arcata API Makefile
# Modular build system for Scala 3 ETL service

.PHONY: help
help:
	@echo "Arcata API Build System"
	@echo ""
	@echo "Build & Run:"
	@echo "  make build        - Compile the project"
	@echo "  make run          - Run the API server"
	@echo "  make run-bg       - Run the API server in background (watch mode)"
	@echo ""
	@echo "Format & Lint:"
	@echo "  make format       - Format code with Scalafmt"
	@echo "  make fmt          - Alias for format"
	@echo "  make format-check - Check formatting without changes"
	@echo "  make lint         - Clean, check format, and run Scalafix linter"
	@echo "  make fix          - Auto-fix formatting and linting issues"
	@echo ""
	@echo "Testing:"
	@echo "  make test         - Run all tests"
	@echo "  make test-only T= - Run specific test (e.g., make test-only T=ConfigTest)"
	@echo ""
	@echo "CI:"
	@echo "  make check        - Run build + test + format-check + lint"
	@echo "  make ci           - Clean build + full check"
	@echo "  make clean        - Remove build artifacts"

include makefiles/build.mk
include makefiles/test.mk
include makefiles/ci.mk
