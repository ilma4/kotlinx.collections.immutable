#!/usr/bin/env python3

import sys
import subprocess
import os
import datetime
import shutil


def get_test_command(testName):
    command = [
        "./gradlew", "--no-daemon",
        ":kotlinx-collections-immutable:cleanJvmTest",
        ":kotlinx-collections-immutable:jvmTest",
        "--tests", "tests.fuzz." + testName
    ]
    return command


def main():
    testName = sys.argv[1]
    subprocess.run("pwd")
    print(testName)

    shutil.rmtree("./core/.cifuzz-corpus")

    command = get_test_command(testName)
    print(command)

    my_env = os.environ.copy()
    my_env["JAZZER_FUZZ"] = "1"

    timestamp = datetime.datetime.now().strftime("%Y-%m-%d--%H-%M-%S")
    f = open(timestamp + "--" + testName, "w")
    subprocess.run(command, env=my_env, stderr=subprocess.STDOUT, stdout=f)
    f.close()


if __name__ == "__main__":
    main()
