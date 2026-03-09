package com.resumeanalyser.frontend.util;

import com.resumeanalyser.frontend.model.AnalysisResult;
import com.resumeanalyser.frontend.model.UserSession;

public class AppState {

    private static UserSession session;
    private static AnalysisResult lastResult;
    private static String lastAnalysisId;
    private static String password;
    private static String jobText;

    public static UserSession getSession() {
        return session;
    }

    public static void setSession(UserSession session) {
        AppState.session = session;
    }

    public static AnalysisResult getLastResult() {
        return lastResult;
    }

    public static void setLastResult(AnalysisResult lastResult) {
        AppState.lastResult = lastResult;
    }

    public static String getLastAnalysisId() {
        return lastAnalysisId;
    }

    public static void setLastAnalysisId(String lastAnalysisId) {
        AppState.lastAnalysisId = lastAnalysisId;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        AppState.password = password;
    }

    public static String getJobText() {
        return jobText;
    }

    public static void setJobText(String jobText) {
        AppState.jobText = jobText;
    }
}
