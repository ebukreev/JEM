# JEM
JVM Exceptions Manager

JEM is a tool that parses jar archives and stores possible exceptions for each method in each class, which are in .class file format.

For more information on how the tool works, see: [jem.pdf](https://github.com/eugenpolytechnic/JEM/blob/master/jem.pdf)

Also, JEM is a plugin for IntelliJ IDEA. After installing the plugin, you need to select the lines with the method calls you want to check and press ctrl + alt + e, then you will see a window with possible exceptions and method calls, that raise it. In addition, the plugin will show you what exceptions can be thrown inside a try block, other than those caught in catch blocks. Currently, analysis is possible for Java and Kotlin languages.