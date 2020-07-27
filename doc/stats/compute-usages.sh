#!/bin/bash
#
# compute-usages.sh
# Copyright © 1993-2020, The Avail Foundation, LLC.
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
#
# * Redistributions in binary form must reproduce the above copyright notice,
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
#
# * Neither the name of the copyright holder nor the names of the contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#

#
# Sincerely determine which external classes and methods are being used by the
# Avail implementation, as a gauge on the level of effort for leaving behind the
# JVM. Deliberately does not attempt to detect pojo usages from Avail.
#
# The script is intended to be run from $AVAIL/doc/stats. Do not try to run it
# from elsewhere.
#
# @author Todd L Smith <todd@availlang.org>
#

ROOT=../..
METHODS=used_methods.txt
CLASSES=used_classes.txt

find $ROOT -name '*.class' -exec javap -c -verbose {} \; \
	| awk '$3 == "Methodref" {print $6}' \
	| grep -v '^com/avail' \
	| grep -v '^"' \
	| sort \
	| uniq \
	> $METHODS

sed -E 's/\..*$//' $METHODS \
	| uniq \
	> $CLASSES
