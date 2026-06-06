#!/bin/sh
set -u

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

pass_count=0
warn_count=0
fail_count=0

say() {
    printf '%s\n' "$*"
}

pass() {
    pass_count=$((pass_count + 1))
    printf 'PASS %s\n' "$*"
}

warn() {
    warn_count=$((warn_count + 1))
    printf 'WARN %s\n' "$*"
}

fail() {
    fail_count=$((fail_count + 1))
    printf 'FAIL %s\n' "$*"
}

check_command() {
    name=$1
    if command -v "$name" >/dev/null 2>&1; then
        path=$(command -v "$name")
        pass "$name found at $path"
        return 0
    fi

    fail "$name not found on PATH"
    return 1
}

local_sdk_dir() {
    props="$ROOT_DIR/local.properties"
    if [ ! -f "$props" ]; then
        return 1
    fi

    sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*//p' "$props" | sed -n '1p'
}

project_compile_sdks() {
    find "$ROOT_DIR" -name build.gradle -type f -exec sed -n 's/^[[:space:]]*compileSdk\(Version\)\{0,1\}[[:space:]]*\([0-9][0-9]*\).*/\2/p' {} \; | sort -n -u
}

sdk_dir=""
sdk_source=""
project_compile_sdks=$(project_compile_sdks)
project_compile_sdks_text=$(printf '%s\n' $project_compile_sdks | tr '\n' ' ' | sed 's/[[:space:]]*$//')

if [ "${ANDROID_HOME:-}" ]; then
    sdk_dir=$ANDROID_HOME
    sdk_source=ANDROID_HOME
elif [ "${ANDROID_SDK_ROOT:-}" ]; then
    sdk_dir=$ANDROID_SDK_ROOT
    sdk_source=ANDROID_SDK_ROOT
else
    detected_sdk_dir=$(local_sdk_dir || true)
    if [ "$detected_sdk_dir" ]; then
        sdk_dir=$detected_sdk_dir
        sdk_source=local.properties
    fi
fi

say "Android build environment check"
say "Project root: $ROOT_DIR"
if [ "$project_compile_sdks" ]; then
    say "Required compile SDKs: $project_compile_sdks_text"
else
    say "Required compile SDKs: unknown"
fi
say ""

say "Java and Gradle"
if check_command java; then
    java_version=$(java -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | sed -n '1p')
    case "$java_version" in
        17.*)
            pass "Java version is $java_version"
            ;;
        "")
            warn "Could not parse Java version"
            ;;
        *)
            warn "Java version is $java_version; this project is verified with Java 17"
            ;;
    esac
fi

if [ -x "$ROOT_DIR/gradlew" ]; then
    pass "Gradle wrapper is executable"
else
    fail "Gradle wrapper is missing or not executable at $ROOT_DIR/gradlew"
fi

say ""
say "Android SDK location"
if [ "$sdk_dir" ]; then
    pass "SDK location comes from $sdk_source: $sdk_dir"
    if [ -d "$sdk_dir" ]; then
        pass "SDK directory exists"
    else
        fail "SDK directory does not exist: $sdk_dir"
    fi
else
    fail "No SDK location set. Set ANDROID_HOME or create an uncommitted local.properties with sdk.dir=/path/to/android-sdk"
fi

say ""
say "SDK packages"
if [ ! "$project_compile_sdks" ]; then
    fail "Could not derive compile SDK versions from Gradle files"
elif [ "$sdk_dir" ] && [ -d "$sdk_dir" ]; then
    for compile_sdk in $project_compile_sdks; do
        platform_dir="$sdk_dir/platforms/android-$compile_sdk"
        if [ -f "$platform_dir/android.jar" ]; then
            pass "Found $platform_dir/android.jar"
        else
            fail "Missing $platform_dir/android.jar"
        fi
    done

    build_tools_dir=""
    if [ -d "$sdk_dir/build-tools" ]; then
        build_tools_dir=$(find "$sdk_dir/build-tools" -mindepth 1 -maxdepth 1 -type d | sort | tail -n 1)
    fi

    if [ "$build_tools_dir" ]; then
        pass "Found build-tools directory: $build_tools_dir"
        for tool in aapt aapt2 aidl apksigner d8 zipalign; do
            if [ -x "$build_tools_dir/$tool" ]; then
                pass "Found build-tools/$tool"
            else
                fail "Missing executable build-tools/$tool in $build_tools_dir"
            fi
        done
    else
        fail "Missing SDK build-tools directory"
    fi

    if [ -x "$sdk_dir/platform-tools/adb" ]; then
        pass "Found platform-tools/adb"
    else
        warn "platform-tools/adb not found; not needed for assembleDebug, but useful for device verification"
    fi

    if [ -d "$sdk_dir/licenses" ]; then
        pass "SDK licenses directory exists"
    else
        warn "SDK licenses directory missing; run sdkmanager --licenses on desktop/CI SDKs"
    fi
else
    fail "Cannot inspect SDK packages until SDK directory exists"
fi

say ""
say "Termux standalone tools"
for tool in aapt aapt2 aidl d8 apksigner adb zipalign sdkmanager; do
    if command -v "$tool" >/dev/null 2>&1; then
        pass "$tool found at $(command -v "$tool")"
    else
        warn "$tool not found on PATH"
    fi
done

say ""
say "Summary: $pass_count pass, $warn_count warn, $fail_count fail"

if [ "$fail_count" -gt 0 ]; then
    exit 1
fi

exit 0
