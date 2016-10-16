#
# Jackson references
#
# - Warning: com.fasterxml.jackson.databind.ext.DOMSerializer: can't find referenced class org.w3c.dom.bootstrap.DOMImplementationRegistry
-dontwarn org.w3c.dom.bootstrap.DOMImplementationRegistry

# - Warning: com.fasterxml.jackson.databind.ext.Java7SupportImpl: can't find referenced class java.beans.Transient
# - Warning: com.fasterxml.jackson.databind.ext.Java7SupportImpl: can't find referenced class java.beans.ConstructorProperties
# => Safe to ignore (http://stackoverflow.com/questions/39425594/jackson-unable-to-load-jdk7-types-on-android)
-dontwarn java.beans.Transient
-dontwarn java.beans.ConstructorProperties


#
# Rx references
#
# - Warning: rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef: can't find referenced class sun.misc.Unsafe
# => Safe to ignore (https://github.com/ReactiveX/RxJava/issues/1415)
-dontwarn sun.misc.Unsafe
