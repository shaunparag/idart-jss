package org.eclipse.jface.util;

// The vendored lib/JasperReports/swtjasperviewer-1.1.0.jar (the report
// preview/export window opened by "View Report") was compiled against the
// original ~2007 JFace, which had this class. It was later removed from
// JFace in favor of org.eclipse.core.runtime.Assert; the modern JFace
// vendored in this project (see lib/JFace/) no longer has it, so opening
// the report viewer throws NoClassDefFoundError. swtjasperviewer is a
// prebuilt third-party jar - its compiled bytecode can't be recompiled
// against the new JFace, so this restores the exact class it was built
// against instead. Behavior below is decompiled byte-for-byte from the
// original org.eclipse.jface.util.Assert (JFace 3.3.1, ~2007), not
// reconstructed from memory, to guarantee identical semantics.
public final class Assert {

	static class AssertionFailedException extends RuntimeException {
		public AssertionFailedException() {
			super();
		}

		public AssertionFailedException(String detail) {
			super(detail);
		}
	}

	private Assert() {
	}

	public static boolean isLegal(boolean expression) {
		return isLegal(expression, "");
	}

	public static boolean isLegal(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException("assertion failed;" + message);
		}
		return expression;
	}

	public static void isNotNull(Object object) {
		isNotNull(object, "");
	}

	public static void isNotNull(Object object, String message) {
		if (object == null) {
			throw new AssertionFailedException("null argument;" + message);
		}
	}

	public static boolean isTrue(boolean expression) {
		return isTrue(expression, "");
	}

	public static boolean isTrue(boolean expression, String message) {
		if (!expression) {
			throw new AssertionFailedException("Assertion failed:" + message);
		}
		return expression;
	}

}
