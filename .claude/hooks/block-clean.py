#!/usr/bin/env python3
"""PreToolUse(Bash, IDE terminal): deny a Gradle `clean` in the checkout the developer's app runs from.

The app runs from the main working tree's build/, and `clean` deletes the classes under it. Clones and linked
worktrees are other checkouts, so `clean` stays allowed there. The command is tokenized like a shell would, so a
mention inside quotes, a heredoc, or a comment is not a call; a directory it cannot resolve counts as this checkout.
"""
import json
import os
import re
import shlex
import subprocess
import sys

GRADLE = {"gradlew", "gradle", "gradlew.bat"}
SHELLS = {"bash", "sh", "zsh"}
PREFIXES = {"env", "time", "nohup", "exec", "command", "builtin", "nice", "sudo"}
CLEAN_TASK = re.compile(r"(:[\w.-]+)*:?clean")
ASSIGNMENT = re.compile(r"[A-Za-z_]\w*=")
HEREDOC = re.compile(r"<<-?\s*(['\"]?)(\w+)\1")


def git(path, *args):
    result = subprocess.run(["git", "-C", path, *args], capture_output=True, text=True)
    return result.stdout.strip() if result.returncode == 0 else None


def physical(path):
    return os.path.realpath(path) if path and os.path.isdir(path) else None


def resolve(base, arg):
    """The directory `cd arg` lands in from base, or None when it cannot be known."""
    arg = os.path.expanduser(os.path.expandvars(arg.strip("`")))
    if "$" in arg or "`" in arg or base is None and not os.path.isabs(arg):
        return None
    return physical(arg if os.path.isabs(arg) else os.path.join(base, arg))


def strip_heredocs(text):
    lines, out, end = text.split("\n"), [], None
    for line in lines:
        if end is not None:
            if line.strip() == end:
                end = None
            continue
        out.append(line)
        match = HEREDOC.search(line)
        if match:
            end = match.group(2)
    return "\n".join(out)


def tokens(text):
    lexer = shlex.shlex(text, posix=True, punctuation_chars=";&|()<>\n")
    lexer.whitespace = " \t\r"
    lexer.whitespace_split = True
    return list(lexer)


def segments(toks):
    """Split tokens into simple commands; drop redirections and their targets."""
    segment, skip = [], False
    for tok in toks:
        if skip:
            skip = False
        elif tok and set(tok) <= set("<>&") and ("<" in tok or ">" in tok):
            skip = True
        elif tok and set(tok) <= set(";&|()\n"):
            if segment:
                yield segment
            segment = []
        else:
            segment.append(tok)
    if segment:
        yield segment


def gradle_targets(words, cwd):
    """For a Gradle call, the directory it runs in, or None; returns False when it has no clean task."""
    target, clean, i = cwd, False, 0
    while i < len(words):
        word = words[i].strip("`")
        if word in ("-x", "--exclude-task", "-p", "--project-dir"):
            if word in ("-p", "--project-dir") and i + 1 < len(words):
                target = resolve(cwd, words[i + 1])
            i += 2
            continue
        if word.startswith("--project-dir="):
            target = resolve(cwd, word.split("=", 1)[1])
        elif not word.startswith("--exclude-task=") and CLEAN_TASK.fullmatch(word):
            clean = True
        i += 1
    return target if clean else False


def denied(text, cwd, protected):
    """True when the command runs a Gradle clean whose directory is (or may be) the protected checkout."""
    try:
        toks = tokens(strip_heredocs(text))
    except ValueError:  # unbalanced quotes: fall back to a plain search
        return bool(re.search(r"gradlew?\b.*\bclean\b", text))
    for words in segments(toks):
        while words:
            while words and (ASSIGNMENT.match(words[0]) or words[0] in PREFIXES):
                words = words[1:]
            if not words:
                break
            if words[0] in ("cd", "pushd"):
                cwd = resolve(cwd, words[1]) if len(words) > 1 else physical(os.path.expanduser("~"))
                words = words[2:]
                continue
            if words[0] in SHELLS and "-c" in words[:-1]:
                if denied(words[words.index("-c") + 1], cwd, protected):
                    return True
                break
            starts = [i for i, w in enumerate(words) if os.path.basename(w.strip("`")) in GRADLE]
            if not starts:
                break
            for n, start in enumerate(starts):
                end = starts[n + 1] if n + 1 < len(starts) else len(words)
                target = gradle_targets(words[start + 1:end], cwd)
                if target is False:
                    continue
                top = physical(git(target, "rev-parse", "--show-toplevel")) if target else None
                if target is None or top == protected:
                    return True
            break
    return False


def main():
    data = json.load(sys.stdin)
    command = (data.get("tool_input") or {}).get("command") or ""
    if "gradle" not in command or "clean" not in command:
        return
    common = git(os.environ.get("CLAUDE_PROJECT_DIR", "."), "rev-parse", "--path-format=absolute", "--git-common-dir")
    if not common:
        return
    protected = physical(os.path.dirname(os.path.realpath(common)))
    if data.get("tool_name") == "Bash":
        cwd = physical(data.get("cwd")) or protected
    else:  # the IDE terminal starts in the IDE project: this checkout unless another is named
        cwd = physical((data.get("tool_input") or {}).get("projectPath")) or protected
    if denied(command, cwd, protected):
        reason = (f"No Gradle clean in {protected}: the developer's running app loads its classes from its build/, "
                  "and clean deletes them. For the clean test gate use the local-ci skill "
                  "(bash .skills/local-ci/scripts/local-ci.sh), which runs in a throwaway clone; a plain ./gradlew test "
                  "here is fine. A clean build of this checkout is for the user to run.")
        json.dump({"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny",
                                          "permissionDecisionReason": reason}}, sys.stdout)


main()
