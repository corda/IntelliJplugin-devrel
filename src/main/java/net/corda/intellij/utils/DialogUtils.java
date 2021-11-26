package net.corda.intellij.utils;

import com.intellij.openapi.ui.Messages;

public final class DialogUtils {

    private static final String TITLE_TEXT = MessageBundle.message("dialog.confirmation");
    private static final String OK_TEXT = MessageBundle.message("dialog.yes");
    private static final String CANCEL_TEXT = MessageBundle.message("dialog.no");

    private DialogUtils() {
    }

    public static void showConfirmation(String message, Runnable next) {
        int result = Messages.showOkCancelDialog(message, TITLE_TEXT, OK_TEXT, CANCEL_TEXT, null);
        if (result == 0) {
            next.run();
        }
    }

    public static void showConfirmation(String message, String button1, Runnable next1, String button2, Runnable next2) {
        int result = Messages.showOkCancelDialog(message, TITLE_TEXT, button1, button2, null);
        if (result == 0) {
            next1.run();
        } else {
            next2.run();
        }
    }
}
