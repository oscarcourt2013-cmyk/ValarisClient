package dev.valarisclient.core.keybind;

/** Human-readable labels for GLFW key codes (ClickGUI keybind editor). */
public final class KeyNames {

    private KeyNames() {
    }

    public static String glfwName(int key) {
        if (key == Keybind.UNBOUND) {
            return "None";
        }
        if (key >= 65 && key <= 90) {
            return String.valueOf((char) key);
        }
        if (key >= 48 && key <= 57) {
            return String.valueOf((char) key);
        }
        if (key >= 290 && key <= 301) {
            return "F" + (key - 289);
        }
        if (key >= 320 && key <= 329) {
            return "Numpad " + (key - 320);
        }
        return switch (key) {
            case 32 -> "Space";
            case 39 -> "'";
            case 44 -> ",";
            case 45 -> "-";
            case 46 -> ".";
            case 47 -> "/";
            case 59 -> ";";
            case 61 -> "=";
            case 91 -> "[";
            case 92 -> "\\";
            case 93 -> "]";
            case 96 -> "`";
            case 256 -> "Esc";
            case 257 -> "Enter";
            case 258 -> "Tab";
            case 259 -> "Backspace";
            case 260 -> "Insert";
            case 261 -> "Delete";
            case 262 -> "Right";
            case 263 -> "Left";
            case 264 -> "Down";
            case 265 -> "Up";
            case 266 -> "Page Up";
            case 267 -> "Page Down";
            case 268 -> "Home";
            case 269 -> "End";
            case 280 -> "Caps Lock";
            case 281 -> "Scroll Lock";
            case 282 -> "Num Lock";
            case 283 -> "Print Screen";
            case 284 -> "Pause";
            case 340 -> "Left Shift";
            case 341 -> "Left Ctrl";
            case 342 -> "Left Alt";
            case 343 -> "Left Super";
            case 344 -> "Right Shift";
            case 345 -> "Right Ctrl";
            case 346 -> "Right Alt";
            case 347 -> "Right Super";
            case 348 -> "Menu";
            default -> "Key " + key;
        };
    }
}
