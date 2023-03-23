package com.github.zheng93775.generator;

class StringAppender {
    private StringBuilder sb = new StringBuilder();

    public StringAppender appendLine() {
        sb.append("\n");
        return this;
    }

    public StringAppender appendLine(int spaceCount, String str) {
        for (int i = 0; i < spaceCount; i++) {
            sb.append(" ");
        }
        sb.append(str);
        sb.append("\n");
        return this;
    }

    public String toString() {
        return sb.toString();
    }
}
