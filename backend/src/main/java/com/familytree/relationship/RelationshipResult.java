package com.familytree.relationship;

/**
 * 关系计算结果
 */
public class RelationshipResult {
    private String label;           // 关系称呼（如"堂哥"、"伯母"）
    private int generationDiff;     // 代差
    private String category;        // 类别：BLOOD / IN_LAW / SPOUSE / SELF

    public RelationshipResult() {}

    public RelationshipResult(String label, int generationDiff, String category) {
        this.label = label;
        this.generationDiff = generationDiff;
        this.category = category;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getGenerationDiff() { return generationDiff; }
    public void setGenerationDiff(int generationDiff) { this.generationDiff = generationDiff; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
