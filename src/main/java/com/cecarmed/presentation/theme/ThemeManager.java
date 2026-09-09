package com.cecarmed.presentation.theme;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

/**
 * Gestor centralizado de temas visuales (Claro / Oscuro) con AtlantaFX.
 */
public final class ThemeManager {

    private static boolean isDarkMode = false;

    private ThemeManager() {
    }

    public static boolean isDarkMode() {
        return isDarkMode;
    }

    public static void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
        if (darkMode) {
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        }
    }

    public static void toggleTheme() {
        setDarkMode(!isDarkMode);
    }
}
