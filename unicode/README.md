Utilities for emitting Unicode tables used by RE2J.

To rebuild the Unicode tables, run:

```
./gradlew :unicode:run -q > java/com/google/re2j/UnicodeTables.java
```

from the project root directory.
