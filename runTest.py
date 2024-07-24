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
    test_name = sys.argv[1]
    subprocess.run("pwd")
    print(test_name)

    corpus_dir = "./core/.cifuzz-corpus/" + test_name
    if os.path.exists(corpus_dir):
        shutil.rmtree("./core/.cifuzz-corpus")

    command = get_test_command(test_name)
    print(command)

    my_env = os.environ.copy()
    my_env["JAZZER_FUZZ"] = "1"

    timestamp = datetime.datetime.now().strftime("%Y-%m-%d--%H-%M-%S")
    f = open(timestamp + "--" + test_name, "w")
    subprocess.run(command, env=my_env, stderr=subprocess.STDOUT, stdout=f)
    f.close()


if __name__ == "__main__":
    main()
