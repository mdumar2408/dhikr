# Project-specific R8 rules (D-046).
# The libraries in use (kotlinx-serialization, Room, Glance, WorkManager, Navigation 3, DataStore)
# ship their own consumer rules, so nothing app-specific is needed yet. Add a rule here only
# for a crash seen in the staging/release build, with a comment saying which.

# Keep file/line info so release crash stack traces can be decoded with the mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
