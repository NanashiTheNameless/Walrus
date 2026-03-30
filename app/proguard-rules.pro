#
# Copyright 2018 Daniel Underhay & Matthew Daley.
#
# This file is part of Walrus.
#
# Walrus is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Walrus is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Walrus.  If not, see <http://www.gnu.org/licenses/>.
#

# We're an open source project, so...
-dontobfuscate

# Keep annotations (for devices and OrmLite, etc.)
-keepattributes *Annotation*

# Keep fields used by Java serialization. Walrus passes operations, cards, and read steps
# through Bundles/Intents as Serializable.
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Work around https://sourceforge.net/p/proguard/bugs/531/#e9ed
-keepclassmembers,allowshrinking class android.support.** {
    !static final <fields>;
}

# Parceler library
-keep interface org.parceler.Parcel
-keep @org.parceler.Parcel class * { *; }
-keep class **$$Parcelable { *; }

# Manual-entry and metadata-driven dialogs instantiate these classes reflectively.
-keep class * extends dev.namelessnanashi.walrus.card.carddata.CardData {
    public <init>();
}
-keep @com.j256.ormlite.table.DatabaseTable class * {
    public <init>();
}
-keepclassmembers class * {
    @com.j256.ormlite.field.DatabaseField <fields>;
}
-keep class * extends androidx.fragment.app.DialogFragment {
    public <init>();
}
-keep class * extends dev.namelessnanashi.walrus.card.carddata.ui.MifareReadStepDialogFragment {
    public <init>();
}
-keep @dev.namelessnanashi.walrus.card.carddata.CardData$Metadata class * { *; }
-keep @dev.namelessnanashi.walrus.device.CardDevice$Metadata class * { *; }
-keep @dev.namelessnanashi.walrus.card.carddata.MifareReadStep$Metadata class * { *; }
-keep interface dev.namelessnanashi.walrus.card.carddata.ui.component.ComponentSourceAndSink
-keep class * implements dev.namelessnanashi.walrus.card.carddata.ui.component.ComponentSourceAndSink { *; }

# Don't warn about these being referenced but not found
-dontwarn com.google.errorprone.**
-dontwarn com.google.gson.**
-dontwarn com.google.j2objc.**
-dontwarn com.sun.jdi.**
-dontwarn java.applet.**
-dontwarn java.lang.**
-dontwarn javax.annotation.**
-dontwarn javax.lang.model.**
-dontwarn javax.persistence.**
-dontwarn javax.servlet.**
-dontwarn javax.tools.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn org.dom4j.**
-dontwarn org.slf4j.**
