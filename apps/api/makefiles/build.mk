# Build, format, and lint targets

# Use local mill wrapper if available, otherwise system mill
MILL := $(shell if [ -x ./mill ]; then echo ./mill; else echo mill; fi)

# Load .env file if it exists
ifneq (,$(wildcard .env))
include .env
export
endif

.PHONY: build run run-bg format fmt format-check lint fix clean

# Compile the project
build:
	$(MILL) api.compile

# Run the API server
run:
	$(MILL) api.run

# Run the API server in background with watch mode
run-bg:
	$(MILL) -w api.runBackground

# Format code with Scalafmt
format:
	$(MILL) mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources

# Alias for format
fmt: format

# Check formatting without making changes
format-check:
	$(MILL) mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources

# Run Scalafix linter (check mode)
# Runs clean first to ensure fresh state
lint: clean
	@echo "Running format check..."
	$(MILL) mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources || (echo "Format issues found. Run 'make format' to fix." && exit 1)
	@echo "Running Scalafix check..."
	$(MILL) api.fixCheck
	$(MILL) api.test.fixCheck

# Auto-fix all linting and formatting issues
fix: format
	$(MILL) api.fix || true
	$(MILL) api.test.fix || true

# Remove build artifacts
clean:
	rm -rf out/
