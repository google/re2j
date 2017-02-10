/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.re2j;

import io.airlift.slice.Slice;
import io.airlift.slice.SliceOutput;

/**
 * Utility methods related to {@link Slice} class.
 */
final class SliceUtils {

  static void appendReplacement(SliceOutput so, Slice replacement, Matcher matcher) {
    int idx = 0;

    // Handle the following items:
    // 1. ${name};
    // 2. $0, $1, $123 (group 123, if exists; or group 12, if exists; or group 1);
    // 3. \\, \$, \t (literal 't').
    // 4. Anything that doesn't starts with \ or $ is considered regular bytes
    while (idx < replacement.length()) {
      byte nextByte = replacement.getByte(idx);
      if (nextByte == '$') {
        idx++;
        if (idx == replacement.length()) {
          throw new IllegalArgumentException("Illegal replacement sequence: " + replacement.toStringUtf8());
        }
        nextByte = replacement.getByte(idx);
        int backref;
        if (nextByte == '{') { // case 1 in the above comment
          idx++;
          int startCursor = idx;
          while (idx < replacement.length()) {
            nextByte = replacement.getByte(idx);
            if (nextByte == '}') {
              break;
            }
            idx++;
          }
          String groupName = replacement.slice(startCursor, idx - startCursor).toStringUtf8();
          Integer namedGroupIndex = matcher.pattern().re2().namedGroupIndexes.get(groupName);
          if (namedGroupIndex == null) {
            throw new IndexOutOfBoundsException("Illegal replacement sequence: unknown group " + groupName);
          }
          backref = namedGroupIndex;
          idx++;
        } else { // case 2 in the above comment
          backref = nextByte - '0';
          if (backref < 0 || backref > 9) {
            throw new IllegalArgumentException("Illegal replacement sequence: " + replacement.toStringUtf8());
          }
          if (matcher.groupCount() < backref) {
            throw new IndexOutOfBoundsException("Illegal replacement sequence: unknown group " + backref);
          }
          idx++;
          while (idx < replacement.length()) { // Adaptive group number: find largest group num that is not greater than actual number of groups
            int nextDigit = replacement.getByte(idx) - '0';
            if (nextDigit < 0 || nextDigit > 9) {
              break;
            }
            int newBackref = (backref * 10) + nextDigit;
            if (matcher.groupCount() < newBackref) {
              break;
            }
            backref = newBackref;
            idx++;
          }
        }
        Slice group = matcher.group(backref);
        if (group != null) {
          so.writeBytes(group);
        }
      } else { // case 3 and 4 in the above comment
        if (nextByte == '\\') {
          idx++;
          if (idx == replacement.length()) {
            throw new IllegalArgumentException("Illegal replacement sequence: " + replacement.toStringUtf8());
          }
          nextByte = replacement.getByte(idx);
        }
        so.appendByte(nextByte);
        idx++;
      }
    }
  }

  private SliceUtils() {
  }
}
