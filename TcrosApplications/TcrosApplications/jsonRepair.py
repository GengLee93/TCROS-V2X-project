import sys
from json_repair import repair_json

def main():
    broken_json = sys.stdin.read()
    try:
        fixed = repair_json(broken_json)
        print(fixed)
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == '__main__':
    main()