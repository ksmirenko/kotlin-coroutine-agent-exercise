package agent

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ASM5
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

/**
 * A [ClassFileTransformer] that detects example.CoroutineExampleKt.test() launches in the given class.
 */
class TestMethodTransformer : ClassFileTransformer {
    override fun transform(loader: ClassLoader?, className: String?,
                           classBeingRedefined: Class<*>?, protectionDomain: ProtectionDomain?,
                           classfileBuffer: ByteArray?): ByteArray {
        val classReader = ClassReader(classfileBuffer)
        // no flags for ClassWriter, as we modify maxs manually and frame count is unchanged
        val classWriter = ClassWriter(classReader, 0)
        classReader.accept(TestDetectorAdapter(classWriter), 0)

        return classWriter.toByteArray()
    }
}

/**
 * A [ClassVisitor] that detects example.CoroutineExampleKt.test() launches
 * and prepends them with printing "Test detected".
 */
class TestDetectorAdapter(
        private val classWriter: ClassWriter
) : ClassVisitor(ASM5, classWriter) {
    override fun visitMethod(access: Int, name: String?, desc: String?,
                             signature: String?, exceptions: Array<out String>?): MethodVisitor {
        val defaultMethodVisitor = classWriter.visitMethod(access, name, desc, signature, exceptions)
        return TestDetectingMethodVisitor(defaultMethodVisitor)
    }

    private class TestDetectingMethodVisitor(defaultMethodVisitor: MethodVisitor)
        : MethodVisitor(ASM5, defaultMethodVisitor) {
        private val TEST_DETECTED_MESSAGE = "Test detected"

        override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
            if (isTestCall(opcode, owner, name, desc)) {
                mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                mv.visitLdcInsn(TEST_DETECTED_MESSAGE)
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                        "(Ljava/lang/String;)V", false)
            }

            mv.visitMethodInsn(opcode, owner, name, desc, itf)
        }

        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
            super.visitMaxs(maxStack + 2, maxLocals)
        }

        private fun isTestCall(opcode: Int, owner: String?, name: String?, desc: String?) =
                (opcode == Opcodes.INVOKESTATIC)
                        && (owner == "example/CoroutineExampleKt")
                        && (name == "test")
                        && (desc == "(Lkotlin/coroutines/experimental/Continuation;)Ljava/lang/Object;")
    }
}
