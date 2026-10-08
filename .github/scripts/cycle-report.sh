#!/bin/bash
# The verdict of a cycle (tools/Cycle.java) in the summary of the run and as an annotation of the job : an error for a
# blocking job, a warning for a non-blocking one or a cycle that passed on its rechecks (annotations are readable
# through the public API without logging in, unlike the logs). Run from the workspace (samples/showcase/, cycle.log,
# install.log) with LABEL, RUNNER and BLOCKING set. The log of the step gets what the artifacts would tell : the
# environment of the runs, the checks and errors, every image that differs and where, the rechecks, the control of
# --trace and the exceptions of the runs.
# Portable : bash 3.2 and the BSD tools of macOS, Git Bash on Windows, GNU tools on Linux. Exits 0 : the cycle step
# fails the job. No head after a command that may write more than a pipe holds : a step may ignore SIGPIPE, and that
# command would then write errors.
c=samples/showcase/comparison
summary=$c/diff-$LABEL/summary.txt
# the files the Java tools write end their lines with CR LF on Windows
verdict=$(grep -m1 -E '^(MATCH|MISMATCH) :' "$c/logs-$LABEL/compare.txt" 2>/dev/null | tr -d '\r')
verdict=${verdict:-no comparison}
# the JVM run compared with the trace run (Cycle.java --trace) : a hint, an image that differs there too suggests timing
# or non-determinism rather than the native image
control=$(grep -m1 '^CONTROL ' cycle.log 2>/dev/null | tr -d '\r')
# the pages that differ run again (Cycle.java) : the verdict of the rechecks
recheck=$(grep -m1 '^RECHECK : ' cycle.log 2>/dev/null | tr -d '\r')
# what failed : the last line of the cycle (a build, the javafx-swt install of the SWT variant, a run, the comparison),
# its arguments, a cycle that did not finish, or the steps before it
failed="" details=""
if [ -f cycle.log ]; then
    failed=$(grep -m1 -E "cycle $LABEL FAILED|Invalid label|Unknown option|usage: " cycle.log | tr -d '\r' \
        | sed 's/^\[[0-9:]*\] //')
    if [ -z "$failed" ] && ! grep -q "cycle $LABEL OK" cycle.log; then
        last=$(grep -E '^\[[0-9:]*\] ' cycle.log | tail -1 | tr -d '\r' | sed 's/^\[[0-9:]*\] //')
        failed="the cycle did not finish (a time-out or a crash), last step : ${last:-none}"
    fi
elif [ -f install.log ]; then
    failed="installing quarkus-fx failed"
else
    failed="the job failed before installing quarkus-fx"
fi
# the Maven log of a failed build or javafx-swt install
case "$failed" in
    *"JVM build failed"*) buildlog=$c/logs-$LABEL/jvm-build.log ;;
    *"native build failed"*) buildlog=$c/logs-$LABEL/native-build.log ;;
    *"javafx-swt could not be installed"*) buildlog=$c/logs-$LABEL/javafx-swt.log ;;
    *) buildlog="" ;;
esac
# the first $1 lines of the input, then how many were left out and where they are ($2) : awk reads the whole input,
# unlike head
first() {
    awk -v n="$1" -v where="$2" 'NR <= n { print } END { if (NR > n) print "... " NR - n " more lines" where }'
}
# the images that differ (with where) and the environment differences of a summary of Compare.java
differing() {
    grep -E '^(DIFFERENT|SIZE|ONLY_A|ONLY_B|ENV DIFF)' "$1" | tr -d '\r'
}
# the page ids with their notes, the expected differences (runtime dependent pages) left out, and the uncaught errors
# outside pages
notes() {
    sed -n '/^== Checks and errors/,$p' "$1" | tr -d '\r' | tail -n +2 \
        | awk '/^uncaught outside pages/ { print; next }
               /^ / { if ($0 !~ /^ *expected: /) { if (id != "") print id; id = ""; print } next } { id = $0 }'
}
if [ -f "$summary" ]; then
    # at most 15 lines of images : the notes say why they differ
    where=" in the log of the Report step"
    details=$({ differing "$summary" | first 15 "$where"; notes "$summary"; } | first 30 "$where")
elif [ -n "$buildlog" ]; then
    details=$(grep -E '^\[ERROR\]|Fatal error|^Error:' "$buildlog" 2>/dev/null | tr -d '\r' \
        | first 15 " in $buildlog")
elif [ "$failed" = "installing quarkus-fx failed" ]; then
    details=$(grep -E '^\[ERROR\]' install.log | tr -d '\r' | first 15 ' in install.log')
fi
# what the runs got : the JavaFX and Java versions, the operating system, the screen and the Prism pipeline
environment=""
k='javafxVersion|javaVersion|os|screen|pipeline'
for run in jvm native; do
    report=$c/$run-$LABEL/report.json
    [ -f "$report" ] || continue
    keys=$(sed -n -E -e "s/^  \"($k)\": \"([^\"]*)\",?\$/\1=\2/p" -e "s/^  \"($k)\": ([^\",]*),?\$/\1=\2/p" "$report" \
        | tr -d '\r' | paste -s -d ';' -)
    environment="$environment$run: $keys
"
done
# what the cycle concluded : why it failed, the rechecks when the pages that differed matched there, or the verdict
outcome=${failed:-${recheck:-$verdict}}
suffix="" level=error
if [ "$BLOCKING" != true ]; then suffix=" (non-blocking)" level=warning; fi
# a cycle that passed on its rechecks : a warning
if [ -z "$failed" ]; then level=warning; fi
if [ -n "$failed" ] || [ "${verdict#MATCH}" = "$verdict" ]; then
    # one line : the % and the line breaks encoded
    message=$({ printf '%s\n' "$outcome"
                if [ -n "$recheck" ] && [ "$recheck" != "$outcome" ]; then printf '%s\n' "$recheck"; fi
                if [ -n "$control" ]; then printf '%s\n' "$control"; fi
                printf '%s\n' "$details"; } \
        | awk 'BEGIN { ORS = "" } { gsub(/%/, "%25"); gsub(/\r/, "%0D"); if (NR > 1) print "%0A"; print }')
    echo "::$level title=Showcase $LABEL on $RUNNER$suffix::$message"
fi
{
    echo "### $LABEL on $RUNNER$suffix"
    echo
    echo "\`$verdict\`"
    if [ -n "$recheck" ]; then echo; echo "\`$recheck\`"; fi
    if [ -n "$control" ]; then echo; echo "\`$control\`"; fi
    if [ -n "$failed" ]; then echo; echo "$failed"; fi
    if [ -n "$environment" ]; then echo; echo '```'; printf '%s' "$environment"; echo '```'; fi
    if [ -n "$details" ]; then echo; echo '```'; echo "$details"; echo '```'; fi
    if [ -f "$c/trace-$LABEL/metadata-diff.md" ]; then
        echo; echo "MetadataDiff (informational, see the artifact):"
        sections=$(grep '^## ' "$c/trace-$LABEL/metadata-diff.md" | tr -d '\r' | sed 's/^## /- /')
        echo "${sections:-- (no MetadataDiff output)}"
    fi
} >> "$GITHUB_STEP_SUMMARY"

# The exceptions of the run.log $1 under the title $2, by block : a line naming an exception or an error and its stack
# trace (the indented lines, the causes, and the trace printed after a message naming the exception, as JavaFX's
# NativeLibLoader does), at most 25 lines of it and then only its causes. Blocks with the same first line (the date and
# time left out) are counted together, the first 5 shown.
exceptions() {
    # C locale : a run.log in the console encoding of Windows is not UTF-8 (gawk warns about invalid multibyte data)
    tr -d '\r\000' < "$1" | LC_ALL=C awk -v title="$2" -v blocks=5 -v lines=25 '
        function keep(line) {
            if (n > lines && line !~ /^[ \t]*Caused by:/) { cut[b]++; return }
            if (cut[b] > 0) text[b] = text[b] "\n      ... " cut[b] " lines"
            cut[b] = 0; text[b] = text[b] "\n    " substr(line, 1, 1000)
        }
        n && (/^[ \t]/ || /^Caused by:/ || (n == 1 && /^[A-Za-z_$][A-Za-z0-9_$.]*(Exception|Error)(:|$)/)) {
            n++; if (b) keep($0); next
        }
        { n = 0 }
        /^[^ \t]/ && /Exception|Error(:|$)|^Caused by:/ {
            found++; n = 1; b = 0; key = $0
            sub(/^([0-9]+-[0-9]+-[0-9]+ )?[0-9][0-9]:[0-9][0-9]:[0-9][0-9][,.0-9]* */, "", key)
            if (!(key in id)) {
                id[key] = ++distinct; at[distinct] = NR
                if (distinct <= blocks) { b = distinct; keep($0) }
            }
            times[id[key]]++
        }
        END {
            print title " : " (found ? found " exception(s) or error(s), " distinct " distinct" : "no exception")
            for (i = 1; i <= distinct && i <= blocks; i++) {
                print "  line " at[i] (times[i] > 1 ? ", " times[i] " times" : "") " :" text[i] \
                    (cut[i] ? "\n      ... " cut[i] " lines" : "")
            }
            if (distinct > blocks) print "  and " distinct - blocks " other distinct one(s)"
        }'
}

# The job log : readable without the artifacts. Workflow commands are off while it prints lines of the runs.
token="cycle-report-$$-$RANDOM"
echo "::stop-commands::$token"
echo "== Showcase $LABEL on $RUNNER$suffix : $outcome"
if [ "$outcome" != "$verdict" ]; then printf '%s\n' "$verdict"; fi
if [ -n "$control" ]; then printf '%s\n' "$control"; fi
if [ -n "$recheck" ]; then grep -E '^RECHECK ' cycle.log | tr -d '\r'; fi
if [ -n "$environment" ]; then printf '%s' "$environment"; fi
if [ -f "$summary" ]; then
    all=$(notes "$summary")
    if [ -n "$all" ]; then
        echo
        echo "== Checks and errors (the expected differences left out)"
        printf '%s\n' "$all" | first 300 " in $summary"
    fi
    echo
    grep -m1 '^== Images' "$summary" | tr -d '\r'
    images=$(differing "$summary")
    printf '%s\n' "${images:-no image differs}"
    if [ -n "$control" ] && [ -f "$c/diff-$LABEL/control/summary.txt" ]; then
        echo
        echo "== Control, a hint : the images that differ between jvm-$LABEL and trace-$LABEL (the JVM under the agent)"
        images=$(differing "$c/diff-$LABEL/control/summary.txt")
        printf '%s\n' "${images:-none}"
    fi
elif [ -n "$details" ]; then
    echo
    printf '%s\n' "$details"
fi
for run in jvm trace native; do
    log=$c/$run-$LABEL/run.log
    if [ ! -f "$log" ]; then
        if [ "$run" != trace ]; then echo; echo "== $run-$LABEL/run.log : none"; fi
        continue
    fi
    # the end of the run (Snapshot.java) : exit=<code>, after WATCHDOG when it was killed (-a : a crash may have written
    # binary data)
    end=$(grep -a -E '^(exit=|WATCHDOG)' "$log" | tr -d '\r' | paste -s -d ' ' -)
    echo
    exceptions "$log" "== $run-$LABEL/run.log (${end:-no exit code})"
    if [ "$end" != "exit=0" ]; then
        echo "  the last lines :"
        tail -15 "$log" | tr -d '\r\000' | cut -c1-1000 | sed 's/^/    /'
    fi
done
echo "::$token::"
exit 0
