#!/usr/bin/env python3
"""
Jasmine to Jest conversion script

This script automatically converts Jasmine test files to Jest with minimal changes:
- Converts jasmine.clock() to jest timer functions
- Updates spy syntax from spyOn to jest.spyOn
- Adds SpecUtil import when SpecUtil is referenced
- Adds MockData import when needed
- Keeps custom matchers like toHaveActionTypesInOrder
- Renames files from *Spec.js to *.jestspec.js
- Makes other minor syntax adjustments

Usage:
  python jasmine2jest.py path/to/jasmineSpec.js [output/path/optional.jestspec.js]
  python jasmine2jest.py --dir path/to/directory
  python jasmine2jest.py path/to/jasmineSpec.js --run-tests --git-stage
  
Note: 
  Run this script from the insight-brain-frontend directory.
  The --dir option recursively searches for files in the specified directory and all subdirectories.
  The --run-tests flag will run Jest tests using yarn.
  The --git-stage flag will stage the new Jest file and remove the old Jasmine file in git,
  but only if --run-tests is also specified and all tests pass. This helps you visualize
  the diff between Jasmine and Jest files in the git index.
"""

import os
import re
import sys
import glob
import argparse
from pathlib import Path

def convert_file_content(content, add_specutil=True):
    """Convert Jasmine test file content to Jest"""
    
    # Check if SpecUtil is actually used in the file
    specutil_used = re.search(r'\bSpecUtil\b', content) is not None
    
    # Check if MockData is used but not imported
    mockdata_used = re.search(r'\bMockData\b', content) is not None
    mockdata_imported = re.search(r"import.*MockData", content) is not None
    
    # Check if SidebarResourceMockData is used but not imported
    sidebar_mockdata_used = re.search(r'\bSidebarResourceMockData\b', content) is not None
    sidebar_mockdata_imported = re.search(r"import.*SidebarResourceMockData", content) is not None
    
    # Handle imports
    import_statements = []
    
    # Add SpecUtil import if it's used and not already imported
    if specutil_used and add_specutil and not re.search(r"import\s+['\"].*SpecUtil['\"]", content):
        import_statements.append("// Import SpecUtil for jasmine compatibility layer\nimport 'TestRoot/SpecUtil';")
    
    # Add MockData import if it's used and not already imported
    if mockdata_used and not mockdata_imported:
        import_statements.append("// Import MockData for jasmine compatibility layer\nimport 'TestRoot/assets/MockData';")
    
    # Add SidebarResourceMockData import if it's used and not already imported
    if sidebar_mockdata_used and not sidebar_mockdata_imported:
        import_statements.append("// Import SidebarResourceMockData for jasmine compatibility layer\nimport 'TestRoot/mock.data/sidebar.resource.mock.data';")
    
    # Add the imports after any existing import statements
    if import_statements:
        # This more sophisticated approach checks for multi-line import statements
        # and ensures we insert new imports after all existing imports are complete
        
        # First, find all import statement blocks, including multi-line ones
        import_matches = list(re.finditer(r"import\s+(?:(?:[^{;]+?from\s+['\"][^'\"]+['\"])|(?:.*?{[^}]*}.*?from\s+['\"][^'\"]+['\"]))\s*;", content, re.DOTALL))
        
        if import_matches:
            # Get the position after the last complete import statement
            last_import = import_matches[-1]
            last_import_pos = last_import.end()
            
            # Insert the additional imports after the last complete import
            content = content[:last_import_pos] + "\n\n" + "\n".join(import_statements) + "\n" + content[last_import_pos:]
        else:
            # No complete imports found, check for the copyright header
            header_match = re.search(r"\/\*[\s\S]*?\*\/", content)
            if header_match:
                content = content[:header_match.end()] + "\n\n" + "\n".join(import_statements) + "\n" + content[header_match.end():]
            else:
                content = "\n".join(import_statements) + "\n\n" + content
    
    # Replace beforeEach/afterEach for spies and timers
    # Just replace jasmine.clock() with Jest timer functions directly
    # Don't try to add or modify afterEach blocks as that creates duplicates

    # Replace clock functions
    content = re.sub(r'jasmine\.clock\(\)\.install\(\);', r'jest.useFakeTimers();', content)
    content = re.sub(r'jasmine\.clock\(\)\.uninstall\(\);', r'jest.useRealTimers();', content)
    content = re.sub(r'jasmine\.clock\(\)\.tick\(([^\)]+)\);', r'jest.advanceTimersByTime(\1);', content)
    
    # Handle jasmine.clock().mockDate() by replacing with Jest equivalent
    content = re.sub(r'jasmine\.clock\(\)\.mockDate\(([^\)]+)\);', r'jest.setSystemTime(\1);', content)
    # In case we missed some jasmine.clock() calls that should be replaced
    content = re.sub(r'jasmine\.clock\(\)', r'jest', content)
    
    # Replace Jasmine-specific matchers with Jest equivalents
    # Replace toHaveSize() with toHaveLength() - Jest's built-in equivalent
    content = re.sub(r'\.toHaveSize\(', r'.toHaveLength(', content)
    
    # Replace Jasmine spy functions with Jest equivalents - be more precise with the regex
    content = re.sub(r'spyOn\s*\(\s*([^,]+?)\s*,\s*[\'"]([^\'"]+)[\'"]\s*\)', r'jest.spyOn(\1, "\2")', content)
    
    # Jasmine spy call tracking to Jest equivalents
    content = re.sub(r'([a-zA-Z0-9_\.]+)\.calls\.count\(\)', r'\1.mock.calls.length', content)
    content = re.sub(r'([a-zA-Z0-9_\.]+)\.calls\.argsFor\((\d+)\)', r'\1.mock.calls[\2]', content)
    content = re.sub(r'([a-zA-Z0-9_\.]+)\.calls\.mostRecent\(\)', r'\1.mock.calls[\1.mock.calls.length-1]', content)
    content = re.sub(r'([a-zA-Z0-9_\.]+)\.calls\.allArgs\(\)', r'\1.mock.calls', content)
    content = re.sub(r'([a-zA-Z0-9_\.]+)\.calls\.all\(\)', r'\1.mock.calls', content)
    content = re.sub(r'([a-zA-Z0-9_\.]+)\.calls\.first\(\)', r'\1.mock.calls[0]', content)

    # Replace jasmine.createSpyObj with jest.fn() for each method
    def replace_spy_obj(match):
        obj_name = match.group(1)
        methods_str = match.group(2).strip("'\"")
        # Handle array of strings or comma-separated string list
        if "[" in methods_str and "]" in methods_str:
            # Extract strings from array notation
            methods_match = re.findall(r"['\"](.*?)['\"]", methods_str)
            methods = methods_match if methods_match else methods_str.split(",")
        else:
            methods = methods_str.split("','")
            
        obj_creation = f"const {obj_name} = {{\n"
        for method in methods:
            method = method.strip("'\" ")
            if method:
                obj_creation += f"    {method.strip()}: jest.fn(),\n"
        obj_creation += "  };"
        return obj_creation

    content = re.sub(r'const\s+(\w+)\s+=\s+jasmine\.createSpyObj\([\'"](?:\w+)[\'"]\s*,\s*\[(.*?)\]\);', replace_spy_obj, content)
    content = re.sub(r'const\s+(\w+)\s+=\s+jasmine\.createSpyObj\([\'"](?:\w+)[\'"]\s*,\s*[\'"]([^\'"]*)[\'"](?:\s*,\s*{.*?})?\);', replace_spy_obj, content)
    
    # Update any jasmine.any() and jasmine.objectContaining() calls to expect.any() and expect.objectContaining()
    content = re.sub(r'jasmine\.any\((.+?)\)', r'expect.any(\1)', content)
    # Make sure we replace jasmine.objectContaining with a more precise regex that can handle multiline matches
    content = re.sub(r'jasmine\.objectContaining\(', r'expect.objectContaining(', content)
    content = re.sub(r'jasmine\.anything\(\)', r'expect.anything()', content)
    
    # Replace toHaveBeenCalledOnceWith() with toHaveBeenCalledTimes(1) and toHaveBeenCalledWith()
    content = re.sub(r'expect\(([^)]+)\)\.toHaveBeenCalledOnceWith\(([^)]+)\)', 
                    r'expect(\1).toHaveBeenCalledTimes(1);\n        expect(\1).toHaveBeenCalledWith(\2)', content)
    
    # Replace jasmine createSpy with jest.fn()
    content = re.sub(r'jasmine\.createSpy\([\'"]([^\'"]+)[\'"]\)', r'jest.fn().mockName("\1")', content)
    content = re.sub(r'jasmine\.createSpy\(\)', r'jest.fn()', content)
    
    # Make sure and.returnValue becomes mockReturnValue
    content = re.sub(r'\.and\.returnValue\(', r'.mockReturnValue(', content)
    content = re.sub(r'\.and\.callFake\(', r'.mockImplementation(', content)
    content = re.sub(r'\.and\.throwError\(', r'.mockImplementation(() => { throw ', content)
    
    # Fix the closing parenthesis for throwError conversion
    content = re.sub(r'\.mockImplementation\(\(\) => { throw (.*?)\);', r'.mockImplementation(() => { throw \1; });', content)
    
    # Convert Jasmine boolean matchers to Jest equivalents
    content = re.sub(r'\.toBeTrue\(\)', r'.toBe(true)', content)
    content = re.sub(r'\.toBeFalse\(\)', r'.toBe(false)', content)
    
    return content

def process_file(input_file, output_file=None):
    """Process a single Jasmine spec file and convert it to Jest
    
    Args:
        input_file: The input Jasmine spec file path
        output_file: The output Jest spec file path (optional)
        
    Returns:
        tuple: (bool, str) A tuple containing success status and output file path
               If a file is skipped (angular.mock or inject-loader), returns (False, None)
               If conversion fails with an error, returns (False, "error")
    """
    if output_file is None:
        # Generate output filename by replacing Spec.js with .jestspec.js
        output_file = re.sub(r'Spec\.jsx?$', '.jestspec.js', input_file)
        # If the filename doesn't match the pattern, append .jestspec.js
        if output_file == input_file:
            base, ext = os.path.splitext(input_file)
            output_file = f"{base}.jestspec.js"
    
    print(f"Converting {input_file} to {output_file}")
    
    try:
        with open(input_file, 'r') as f:
            content = f.read()
        
        # Skip files that use angular.mock as they aren't compatible with Jest
        if re.search(r'\bangular\.mock\b', content):
            print(f"⚠️  Skipping {input_file}: Contains angular.mock which is not compatible with Jest")
            return False, None
            
        # Skip files that use inject-loader as they aren't compatible with Jest
        if re.search(r'inject-loader', content):
            print(f"⚠️  Skipping {input_file}: Contains inject-loader which is not compatible with Jest")
            return False, None
            
        converted_content = convert_file_content(content)
        
        with open(output_file, 'w') as f:
            f.write(converted_content)
            
        print(f"✓ Successfully converted {input_file}")
        return True, output_file
    except Exception as e:
        print(f"✗ Error converting {input_file}: {str(e)}")
        return False, "error"

def process_directory(directory_path, file_pattern='*Spec.js', run_tests=False, git_stage=False):
    """Process all Jasmine spec files in a directory and its subdirectories"""
    success_count = 0
    fail_count = 0
    skipped_count = 0
    converted_files = []
    
    # Use Path.rglob to find files recursively in subdirectories
    spec_files = list(Path(directory_path).rglob(file_pattern))
    
    # If no files found with exact pattern, try a more lenient search
    if not spec_files:
        # For file patterns like "someSpec.js", we need to be more flexible
        if file_pattern.startswith('*'):
            spec_files = list(Path(directory_path).rglob(file_pattern))
        else:
            spec_files = list(Path(directory_path).rglob(f"*{file_pattern}"))
    
    for file_path in spec_files:
        file_path_str = str(file_path)
        # Skip already converted files
        if '.jestspec.js' in file_path_str:
            continue
            
        output_file = re.sub(r'Spec\.jsx?$', '.jestspec.js', file_path_str)
        if output_file == file_path_str:
            base, ext = os.path.splitext(file_path_str)
            output_file = f"{base}.jestspec.js"
            
        success, actual_output_file = process_file(file_path_str, output_file)
        if success:
            success_count += 1
            converted_files.append((file_path_str, actual_output_file))
        elif actual_output_file is None:
            # The file was skipped due to angular.mock or inject-loader
            skipped_count += 1
        else:
            fail_count += 1
    
    # Run tests on all successfully converted files at once if requested
    all_tests_passed = True
    if run_tests and converted_files:
        print("\nRunning Jest tests on all converted files:")
        # Get all relative paths for converted files
        relative_paths = [os.path.relpath(output_file, os.getcwd()) for _, output_file in converted_files]
        # Run Jest on all files at once
        test_passed = run_jest_tests_batch(relative_paths)
        if not test_passed:
            all_tests_passed = False
            print("❌ Some tests failed")
        else:
            print("✅ All tests passed")
    
    # Stage changes in git if requested, but only if --run-tests was specified and all tests passed
    if git_stage and converted_files:
        if run_tests:
            if all_tests_passed:
                print("\nAll tests passed! Staging changes in git:")
                for input_file, output_file in converted_files:
                    os.system(f"git add {output_file}")
                    os.system(f"git rm {input_file}")
                    print(f"Git: Added {output_file} and removed {input_file}")
            else:
                print("\nSome tests failed. Not staging changes in git.")
                print("Fix the failing tests and try again.")
        else:
            print("\nWarning: --git-stage requires --run-tests to be specified. Skipping git staging.")
            print("Run with --run-tests --git-stage to automatically stage changes when tests pass.")
            
    print(f"\nConversion complete: {success_count} files successfully converted, {fail_count} failed, {skipped_count} skipped (angular.mock or inject-loader)")

def run_jest_tests(file_path):
    """Run Jest tests on the converted file using yarn
    
    Returns:
        bool: True if tests passed, False otherwise
    """
    # Extract the relative path from the absolute path
    # This assumes you're running the script from insight-brain-frontend directory
    relative_path = os.path.relpath(file_path, os.getcwd())
    
    # Run the Jest test using yarn
    print(f"Running Jest tests on {relative_path}")
    result = os.system(f"cd {os.getcwd()} && yarn jest {relative_path} --no-cache")
    
    # Check if tests passed (exit code 0)
    return result == 0

def run_jest_tests_batch(file_paths):
    """Run Jest tests on a batch of converted files using yarn
    
    Args:
        file_paths (list): List of file paths to run tests on
    
    Returns:
        bool: True if all tests passed, False if any test failed
    """
    # Join all file paths with a space for the command
    files_arg = " ".join(file_paths)
    # Run Jest on all files at once
    result = os.system(f"cd {os.getcwd()} && yarn jest {files_arg} --no-cache")
    return result == 0

def main():
    parser = argparse.ArgumentParser(description='Convert Jasmine specs to Jest')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('input_file', nargs='?', help='Input Jasmine spec file')
    group.add_argument('--dir', help='Directory containing Jasmine specs to convert')
    parser.add_argument('output_file', nargs='?', help='Output Jest spec file (optional)')
    parser.add_argument('--run-tests', action='store_true', help='Run Jest tests on converted files')
    parser.add_argument('--git-stage', action='store_true', 
                        help='Stage changes in git (add new file, remove old file) if tests pass. Requires --run-tests.')
    
    args = parser.parse_args()
    
    # Make sure we're in the insight-brain-frontend directory
    if not os.getcwd().endswith('insight-brain-frontend'):
        print("Warning: This script should be run from the insight-brain-frontend directory.")
    
    if args.dir:
        process_directory(args.dir, run_tests=args.run_tests, git_stage=args.git_stage)
    else:
        # Set initial output file path
        output_file = args.output_file
        
        # Convert the file
        success, actual_output_file = process_file(args.input_file, output_file)
        
        # Use the actual output file path returned by process_file
        if success:
            tests_passed = True
            if args.run_tests:
                print(f"\nRunning Jest tests on {actual_output_file}:")
                tests_passed = run_jest_tests(actual_output_file)
                if tests_passed:
                    print(f"✅ Tests passed for {actual_output_file}")
                else:
                    print(f"❌ Tests failed for {actual_output_file}")
                
            if args.git_stage:
                if args.run_tests:
                    if tests_passed:
                        # Stage the changes in git
                        print("\nTests passed! Staging changes in git:")
                        os.system(f"git add {actual_output_file}")
                        os.system(f"git rm {args.input_file}")
                        print(f"Git: Added {actual_output_file} and removed {args.input_file}")
                    else:
                        print("\nTests failed. Not staging changes in git.")
                        print("Fix the failing tests and try again.")
                else:
                    print("\nWarning: --git-stage requires --run-tests to be specified. Skipping git staging.")
                    print("Run with --run-tests --git-stage to automatically stage changes when tests pass.")
                
            print(f"\nConversion successful: {args.input_file} → {actual_output_file}")
        elif actual_output_file is None:
            # File was skipped due to angular.mock or inject-loader
            print(f"\nSkipped file: {args.input_file} (contains incompatible features)")
        else:
            # Conversion failed with an error
            print(f"\nConversion failed: {args.input_file}")

if __name__ == "__main__":
    main()
