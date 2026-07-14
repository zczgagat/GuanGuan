package com.familytree.relationship;

import com.familytree.graph.FamilyGraph;
import com.familytree.model.Person;
import com.familytree.model.enums.Gender;

import java.time.LocalDate;
import java.util.*;

/**
 * 关系分类器。
 * 根据 FamilyGraph.RelationshipPath 中的代差、性别、路径信息判定具体关系名称。
 *
 * 覆盖七代以内的所有 Chinese 家族关系。
 */
public class RelationshipClassifier {

    private final FamilyGraph graph;

    // 祖先辈分前缀映射
    private static final Map<Integer, String> ANCESTOR_PREFIX = new LinkedHashMap<>();
    static {
        ANCESTOR_PREFIX.put(3, "曾");
        ANCESTOR_PREFIX.put(4, "高");
        ANCESTOR_PREFIX.put(5, "天");
        ANCESTOR_PREFIX.put(6, "烈");
        ANCESTOR_PREFIX.put(7, "太");
    }

    // 后代辈分前缀映射
    private static final Map<Integer, String> DESCENDANT_PREFIX = new LinkedHashMap<>();
    static {
        DESCENDANT_PREFIX.put(3, "曾");
        DESCENDANT_PREFIX.put(4, "玄");
        DESCENDANT_PREFIX.put(5, "来");
        DESCENDANT_PREFIX.put(6, "晜");
        DESCENDANT_PREFIX.put(7, "仍");
    }

    // 姻亲映射：血亲关系 → 其配偶的称呼
    private static final Map<String, Map<Gender, String>> SPOUSE_OF_RELATIVE = new LinkedHashMap<>();
    static {
        // 长辈/平辈血亲的配偶
        SPOUSE_OF_RELATIVE.put("父亲", Map.of(Gender.FEMALE, "母亲"));
        SPOUSE_OF_RELATIVE.put("母亲", Map.of(Gender.MALE, "父亲"));
        SPOUSE_OF_RELATIVE.put("伯伯", Map.of(Gender.FEMALE, "伯母"));
        SPOUSE_OF_RELATIVE.put("叔叔", Map.of(Gender.FEMALE, "婶婶"));
        SPOUSE_OF_RELATIVE.put("姑姑", Map.of(Gender.MALE, "姑父"));
        SPOUSE_OF_RELATIVE.put("舅舅", Map.of(Gender.FEMALE, "舅妈"));
        SPOUSE_OF_RELATIVE.put("姨", Map.of(Gender.MALE, "姨父"));
        SPOUSE_OF_RELATIVE.put("哥哥", Map.of(Gender.FEMALE, "嫂子"));
        SPOUSE_OF_RELATIVE.put("弟弟", Map.of(Gender.FEMALE, "弟媳"));
        SPOUSE_OF_RELATIVE.put("姐姐", Map.of(Gender.MALE, "姐夫"));
        SPOUSE_OF_RELATIVE.put("妹妹", Map.of(Gender.MALE, "妹夫"));
        SPOUSE_OF_RELATIVE.put("儿子", Map.of(Gender.FEMALE, "儿媳"));
        SPOUSE_OF_RELATIVE.put("女儿", Map.of(Gender.MALE, "女婿"));
        SPOUSE_OF_RELATIVE.put("孙子", Map.of(Gender.FEMALE, "孙媳"));
        SPOUSE_OF_RELATIVE.put("孙女", Map.of(Gender.MALE, "孙女婿"));
        // 祖辈
        SPOUSE_OF_RELATIVE.put("爷爷", Map.of(Gender.FEMALE, "奶奶"));
        SPOUSE_OF_RELATIVE.put("奶奶", Map.of(Gender.MALE, "爷爷"));
        SPOUSE_OF_RELATIVE.put("姥爷", Map.of(Gender.FEMALE, "姥姥"));
        SPOUSE_OF_RELATIVE.put("姥姥", Map.of(Gender.MALE, "姥爷"));
    }

    // 配偶的亲属 → 自己的称呼（根据说话者性别）
    private static final Map<String, Map<Gender, String>> RELATIVE_OF_SPOUSE = new LinkedHashMap<>();
    static {
        // 配偶的直系长辈
        Map<Gender, String> fatherMap = new HashMap<>();
        fatherMap.put(Gender.MALE, "岳父");
        fatherMap.put(Gender.FEMALE, "公公");
        RELATIVE_OF_SPOUSE.put("父亲", fatherMap);

        Map<Gender, String> motherMap = new HashMap<>();
        motherMap.put(Gender.MALE, "岳母");
        motherMap.put(Gender.FEMALE, "婆婆");
        RELATIVE_OF_SPOUSE.put("母亲", motherMap);

        // 配偶的兄弟姐妹
        Map<Gender, String> olderBrotherMap = new HashMap<>();
        olderBrotherMap.put(Gender.MALE, "大舅子");
        olderBrotherMap.put(Gender.FEMALE, "大伯子");
        RELATIVE_OF_SPOUSE.put("哥哥", olderBrotherMap);

        Map<Gender, String> youngerBrotherMap = new HashMap<>();
        youngerBrotherMap.put(Gender.MALE, "小舅子");
        youngerBrotherMap.put(Gender.FEMALE, "小叔子");
        RELATIVE_OF_SPOUSE.put("弟弟", youngerBrotherMap);

        Map<Gender, String> olderSisterMap = new HashMap<>();
        olderSisterMap.put(Gender.MALE, "大姨子");
        olderSisterMap.put(Gender.FEMALE, "大姑子");
        RELATIVE_OF_SPOUSE.put("姐姐", olderSisterMap);

        Map<Gender, String> youngerSisterMap = new HashMap<>();
        youngerSisterMap.put(Gender.MALE, "小姨子");
        youngerSisterMap.put(Gender.FEMALE, "小姑子");
        RELATIVE_OF_SPOUSE.put("妹妹", youngerSisterMap);

        // 配偶的祖辈
        Map<Gender, String> grandpaMap = new HashMap<>();
        grandpaMap.put(Gender.MALE, "岳祖父");
        grandpaMap.put(Gender.FEMALE, "祖父公");
        RELATIVE_OF_SPOUSE.put("爷爷", grandpaMap);

        Map<Gender, String> grandmaMap = new HashMap<>();
        grandmaMap.put(Gender.MALE, "岳祖母");
        grandmaMap.put(Gender.FEMALE, "祖母婆");
        RELATIVE_OF_SPOUSE.put("奶奶", grandmaMap);
    }

    public RelationshipClassifier(FamilyGraph graph) {
        this.graph = graph;
    }

    /**
     * 根据路径计算 A 对 B 的关系称呼
     */
    public RelationshipResult classify(FamilyGraph.RelationshipPath path) {
        if (path == null) {
            return new RelationshipResult("无关系", 0, "UNKNOWN");
        }

        Long a = path.personA;
        Long b = path.personB;

        if (a.equals(b)) {
            return new RelationshipResult("自己", 0, "SELF");
        }

        Gender aGender = graph.getGender(a);
        Gender bGender = graph.getGender(b);

        // 处理配偶路径
        if (path.hasSpouseEdge && path.spouseConnectorId != null) {
            return classifyInLaw(path, aGender);
        }

        int genDiff = path.generationDiff;

        // 直系关系（没有共同祖先，直接是祖先后代）
        if (path.ncaId == null) {
            if (genDiff > 0) {
                // B 是 A 的祖先
                return classifyDirectAncestor(genDiff, bGender, path.isPaternal);
            } else {
                // A 是 B 的祖先
                return classifyDirectDescendant(-genDiff, bGender, path.isPaternal);
            }
        }

        // 有共同祖先
        if (genDiff > 0) {
            // B 是长辈
            return classifyElder(path, a, b, genDiff, bGender);
        } else if (genDiff < 0) {
            // B 是晚辈
            return classifyYounger(path, a, b, -genDiff, bGender);
        } else {
            // 同辈
            return classifySameGeneration(path, a, b, aGender, bGender);
        }
    }

    /**
     * 计算 A 对 B 的关系（便捷方法）
     */
    public RelationshipResult calculate(Long personA, Long personB, int maxGenerations) {
        FamilyGraph.RelationshipPath path = graph.findPath(personA, personB, maxGenerations);
        return classify(path);
    }

    // ==================== 长辈分类 ====================

    private RelationshipResult classifyElder(FamilyGraph.RelationshipPath path, Long a, Long b,
                                              int genDiff, Gender bGender) {
        if (genDiff == 1) {
            // 父母辈
            // 检查是否直系父母
            if (path.upSteps == 1 && path.downSteps == 0) {
                return new RelationshipResult(bGender == Gender.MALE ? "父亲" : "母亲", 1, "BLOOD");
            }

            // 旁系：父母的兄弟姐妹
            // 确定是通过父亲还是母亲
            // 检查 A 到 NCA 的路径中第一个 step 是父亲还是母亲
            Long fatherOfA = graph.getFather(a);
            Long ncaId = path.ncaId;

            boolean throughFather = false;
            if (fatherOfA != null && ncaId != null) {
                // 检查从 A 的父亲到 NCA 的路径
                Map<Long, FamilyGraph.AncestorInfo> ancestorsOfFather = graph.findAllAncestors(fatherOfA, 7);
                throughFather = ancestorsOfFather.containsKey(ncaId);
            }

            if (throughFather) {
                // 父亲的兄弟姐妹
                if (bGender == Gender.MALE) {
                    // 伯伯或叔叔：按年龄区分
                    if (hasAgeOrder(a, fatherOfA, b)) {
                        return new RelationshipResult("伯伯", 1, "BLOOD");
                    } else {
                        return new RelationshipResult("叔叔", 1, "BLOOD");
                    }
                } else {
                    return new RelationshipResult("姑姑", 1, "BLOOD");
                }
            } else {
                // 母亲的兄弟姐妹
                if (bGender == Gender.MALE) {
                    return new RelationshipResult("舅舅", 1, "BLOOD");
                } else {
                    return new RelationshipResult("姨", 1, "BLOOD");
                }
            }
        }

        if (genDiff == 2) {
            // 祖父母辈
            Long fatherOfA = graph.getFather(a);
            Long ncaId = path.ncaId;

            // 判断父系还是母系
            boolean paternal = false;
            if (fatherOfA != null) {
                Map<Long, FamilyGraph.AncestorInfo> ancestorsOfFather = graph.findAllAncestors(fatherOfA, 7);
                // NCA 在父亲的祖先中，或者 NCA 就是父亲
                if (fatherOfA.equals(ncaId) || ancestorsOfFather.containsKey(ncaId)) {
                    paternal = true;
                }
            }

            if (paternal) {
                return new RelationshipResult(bGender == Gender.MALE ? "爷爷" : "奶奶", 2, "BLOOD");
            } else {
                return new RelationshipResult(bGender == Gender.MALE ? "姥爷" : "姥姥", 2, "BLOOD");
            }
        }

        if (genDiff >= 3 && genDiff <= 7) {
            // 曾祖及以上
            String prefix = ANCESTOR_PREFIX.getOrDefault(genDiff, "太");
            String suffix = bGender == Gender.MALE ? "祖父" : "祖母";

            // 判断父系母系
            boolean paternal = path.isPaternal;
            if (paternal) {
                return new RelationshipResult(prefix + suffix, genDiff, "BLOOD");
            } else {
                return new RelationshipResult("外" + prefix + suffix, genDiff, "BLOOD");
            }
        }

        return new RelationshipResult("长辈(" + genDiff + "代)", genDiff, "BLOOD");
    }

    // ==================== 晚辈分类 ====================

    private RelationshipResult classifyYounger(FamilyGraph.RelationshipPath path, Long a, Long b,
                                                int genDiff, Gender bGender) {
        if (genDiff == 1) {
            // 子女辈
            // 检查是否直系子女
            if (path.downSteps == 1 && path.upSteps == 0) {
                // 手动检查是否直系
                Set<Long> childrenOfA = graph.getChildren(a);
                if (childrenOfA.contains(b)) {
                    return new RelationshipResult(bGender == Gender.MALE ? "儿子" : "女儿", -1, "BLOOD");
                }
            }

            // 旁系：兄弟姐妹的子女
            // 确定是兄弟的还是姐妹的
            Long ncaId = path.ncaId;
            // 如果 NCA 是 A 的父母，则 B 是 A 的兄弟姐妹的子女
            // 需要确定 A 的兄弟姐妹的性别

            // 我们找一下从 NCA 到 B 的中间人
            // 通过查 B 的父母来确定
            Long fatherOfB = graph.getFather(b);

            boolean isBrotherLine = false;
            if (fatherOfB != null && ncaId != null) {
                Map<Long, FamilyGraph.AncestorInfo> ancestorsOfB = graph.findAllAncestors(fatherOfB, 7);
                if (fatherOfB.equals(ncaId) || ancestorsOfB.containsKey(ncaId)) {
                    // B 的父亲（即 A 的兄弟）在 NCA 的路径上
                    // 那 A 的兄弟姐妹就是 B 的父亲 = fatherOfB
                    isBrotherLine = true;
                }
            }

            if (isBrotherLine) {
                return new RelationshipResult(bGender == Gender.MALE ? "侄子" : "侄女", -1, "BLOOD");
            } else {
                return new RelationshipResult(bGender == Gender.MALE ? "外甥" : "外甥女", -1, "BLOOD");
            }
        }

        if (genDiff == 2) {
            // 孙辈
            // 判断是儿子的孩子还是女儿的孩子（即是否同姓）
            // A 的孩子的孩子的性别
            Set<Long> childrenOfA = graph.getChildren(a);
            boolean throughSon = false;
            for (Long childId : childrenOfA) {
                Set<Long> grandChildren = graph.getChildren(childId);
                if (grandChildren.contains(b)) {
                    Gender childGender = graph.getGender(childId);
                    if (childGender == Gender.MALE) {
                        throughSon = true;
                    }
                    break;
                }
            }

            if (throughSon) {
                return new RelationshipResult(bGender == Gender.MALE ? "孙子" : "孙女", -2, "BLOOD");
            } else {
                return new RelationshipResult(bGender == Gender.MALE ? "外孙" : "外孙女", -2, "BLOOD");
            }
        }

        if (genDiff >= 3 && genDiff <= 7) {
            String prefix = DESCENDANT_PREFIX.getOrDefault(genDiff, "玄");
            String suffix = bGender == Gender.MALE ? "孙" : "孙女";

            // 判断是否同姓（父系）
            boolean paternal = path.isPaternal;
            if (paternal) {
                return new RelationshipResult(prefix + suffix, -genDiff, "BLOOD");
            } else {
                return new RelationshipResult("外" + prefix + suffix, -genDiff, "BLOOD");
            }
        }

        return new RelationshipResult("晚辈(" + genDiff + "代)", -genDiff, "BLOOD");
    }

    // ==================== 同辈分类 ====================

    private RelationshipResult classifySameGeneration(FamilyGraph.RelationshipPath path, Long a, Long b,
                                                       Gender aGender, Gender bGender) {
        // 亲兄弟姐妹：共同祖先是父母（A 到 parent 再到 B）
        if (path.upSteps == 1 && path.downSteps == 1) {
            // isOlder(a,b)=true 表示 A 比 B 年长，则 B 是 A 的弟/妹
            if (bGender == Gender.MALE) {
                return new RelationshipResult(isOlder(a, b) ? "弟弟" : "哥哥", 0, "BLOOD");
            } else {
                return new RelationshipResult(isOlder(a, b) ? "妹妹" : "姐姐", 0, "BLOOD");
            }
        }

        // 堂/表兄弟姐妹：共同祖先是祖辈或以上
        boolean allMalePath = checkPathAllMale(a, b, path);
        String prefix = allMalePath ? "堂" : "表";

        if (bGender == Gender.MALE) {
            return new RelationshipResult(prefix + (isOlder(a, b) ? "弟" : "哥"), 0, "BLOOD");
        } else {
            return new RelationshipResult(prefix + (isOlder(a, b) ? "妹" : "姐"), 0, "BLOOD");
        }
    }

    // ==================== 姻亲分类 ====================

    private RelationshipResult classifyInLaw(FamilyGraph.RelationshipPath path, Gender aGender) {
        Long a = path.personA;
        Long b = path.personB;
        Long spouseConnector = path.spouseConnectorId;
        Gender bGender = graph.getGender(b);

        // 检查 B 是否是配偶
        Long spouseOfA = graph.getSpouse(a);
        if (spouseOfA != null && spouseOfA.equals(b)) {
            return new RelationshipResult(bGender == Gender.MALE ? "丈夫" : "妻子", 0, "SPOUSE");
        }

        // B 是配偶的血亲
        if (spouseConnector != null && spouseConnector.equals(spouseOfA)) {
            // 计算配偶到 B 的关系
            FamilyGraph.RelationshipPath spouseToBPath = graph.findPath(spouseOfA, b, 7);
            if (spouseToBPath != null) {
                RelationshipResult relFromSpouse = classify(spouseToBPath);
                String label = relFromSpouse.getLabel();

                // 查映射表
                Map<Gender, String> mapping = RELATIVE_OF_SPOUSE.get(label);
                if (mapping != null && mapping.containsKey(aGender)) {
                    return new RelationshipResult(mapping.get(aGender), relFromSpouse.getGenerationDiff(), "IN_LAW");
                }

                // 特殊处理：配偶的兄弟姐妹的子女
                if (label.contains("侄子") || label.contains("侄女") ||
                    label.contains("外甥") || label.contains("外甥女")) {
                    String prefix = aGender == Gender.MALE ? "妻" : "夫";
                    return new RelationshipResult(prefix + label, relFromSpouse.getGenerationDiff() - 1, "IN_LAW");
                }
            }
        }

        // B 是 A 血亲的配偶（如叔叔的妻子 = 婶婶）
        // spouseConnector 是血亲，B 是其配偶
        if (spouseConnector != null) {
            FamilyGraph.RelationshipPath aToConnectorPath = graph.findPath(a, spouseConnector, 7);
            if (aToConnectorPath != null) {
                RelationshipResult relToConnector = classify(aToConnectorPath);
                String label = relToConnector.getLabel();

                Map<Gender, String> mapping = SPOUSE_OF_RELATIVE.get(label);
                if (mapping != null && mapping.containsKey(bGender)) {
                    return new RelationshipResult(mapping.get(bGender), relToConnector.getGenerationDiff(), "IN_LAW");
                }
            }
        }

        // 妯娌/连襟
        if (aGender == Gender.FEMALE && bGender == Gender.FEMALE) {
            return new RelationshipResult("妯娌", 0, "IN_LAW");
        }
        if (aGender == Gender.MALE && bGender == Gender.MALE) {
            return new RelationshipResult("连襟", 0, "IN_LAW");
        }

        return new RelationshipResult("姻亲", path.generationDiff, "IN_LAW");
    }

    // ==================== 辅助方法 ====================

    /**
     * 判断 B 是否比 reference 年长
     * 用于区分伯伯还是叔叔
     */
    private boolean hasAgeOrder(Long personA, Long reference, Long b) {
        if (reference == null || b == null) return false;

        LocalDate refBirth = graph.getPerson(reference) != null ? graph.getPerson(reference).getBirthDate() : null;
        LocalDate bBirth = graph.getPerson(b) != null ? graph.getPerson(b).getBirthDate() : null;

        if (refBirth != null && bBirth != null) {
            return bBirth.isBefore(refBirth); // B 比 reference 年长
        }

        // 没有生日数据，用 ID 比较（ID 小的创建早）
        return b < reference;
    }

    private boolean isOlder(Long a, Long b) {
        Person pa = graph.getPerson(a);
        Person pb = graph.getPerson(b);
        if (pa == null || pb == null) return false;

        // 优先使用 siblingRank（1=老大）
        Integer rankA = pa.getSiblingRank();
        Integer rankB = pb.getSiblingRank();
        if (rankA != null && rankB != null) {
            return rankA < rankB; // 排名更小（更早出生）的为年长
        }

        // 再用出生日期
        LocalDate birthA = pa.getBirthDate();
        LocalDate birthB = pb.getBirthDate();
        if (birthA != null && birthB != null) {
            return birthA.isBefore(birthB);
        }

        // 最后按 ID
        return a < b;
    }

    private String getAgeOrder(Long a, Long b) {
        return isOlder(a, b) ? "older" : "younger";
    }

    /**
     * 检查从 A 到 B 的完整路径（排除 A 和 B）中所有中间人是否为男性。
     * 用于区分 堂/表。
     */
    private boolean checkPathAllMale(Long a, Long b, FamilyGraph.RelationshipPath path) {
        Long ncaId = path.ncaId;
        if (ncaId == null) return true;

        // 检查 A 到 NCA 路径上的中间人
        Set<Long> visited = new HashSet<>();
        visited.add(a);
        visited.add(ncaId);

        // 从 A 向上到 NCA
        Long current = a;
        Set<Long> parents = graph.getParents(current);
        while (!parents.isEmpty() && !parents.contains(ncaId)) {
            // 找在 NCA 路径上的父母
            Long next = findOnPathToNCA(parents, ncaId);
            if (next == null) break;
            if (graph.getGender(next) == Gender.FEMALE) return false;
            visited.add(next);
            current = next;
            parents = graph.getParents(current);
        }

        // 从 NCA 向下到 B
        current = b;
        Set<Long> children = graph.getChildren(current);
        while (!children.isEmpty() && !children.contains(ncaId)) {
            Long next = findOnPathFromNCA(children, ncaId);
            if (next == null) break;
            if (graph.getGender(next) == Gender.FEMALE) return false;
            visited.add(next);
            current = next;
            children = graph.getChildren(current);
        }

        return true;
    }

    private Long findOnPathToNCA(Set<Long> candidates, Long ncaId) {
        for (Long c : candidates) {
            Map<Long, FamilyGraph.AncestorInfo> ancestors = graph.findAllAncestors(c, 7);
            if (ancestors.containsKey(ncaId) || c.equals(ncaId)) {
                return c;
            }
        }
        return candidates.isEmpty() ? null : candidates.iterator().next();
    }

    private Long findOnPathFromNCA(Set<Long> candidates, Long ncaId) {
        for (Long c : candidates) {
            Map<Long, FamilyGraph.AncestorInfo> ancestors = graph.findAllAncestors(c, 7);
            if (ancestors.containsKey(ncaId) || c.equals(ncaId)) {
                return c;
            }
        }
        return candidates.isEmpty() ? null : candidates.iterator().next();
    }

    private RelationshipResult classifyDirectAncestor(int genDiff, Gender gender, boolean paternal) {
        if (genDiff == 1) {
            return new RelationshipResult(gender == Gender.MALE ? "父亲" : "母亲", genDiff, "BLOOD");
        }
        if (genDiff == 2) {
            return new RelationshipResult(
                gender == Gender.MALE ? (paternal ? "爷爷" : "姥爷") :
                                        (paternal ? "奶奶" : "姥姥"),
                genDiff, "BLOOD");
        }
        if (genDiff >= 3 && genDiff <= 7) {
            String prefix = ANCESTOR_PREFIX.getOrDefault(genDiff, "太");
            String suffix = gender == Gender.MALE ? "祖父" : "祖母";
            return new RelationshipResult(
                paternal ? prefix + suffix : "外" + prefix + suffix,
                genDiff, "BLOOD");
        }
        return new RelationshipResult("祖先(" + genDiff + "代)", genDiff, "BLOOD");
    }

    private RelationshipResult classifyDirectDescendant(int genDiff, Gender gender, boolean paternal) {
        if (genDiff == 1) {
            return new RelationshipResult(gender == Gender.MALE ? "儿子" : "女儿", -genDiff, "BLOOD");
        }
        if (genDiff == 2) {
            return new RelationshipResult(
                gender == Gender.MALE ? (paternal ? "孙子" : "外孙") :
                                        (paternal ? "孙女" : "外孙女"),
                -genDiff, "BLOOD");
        }
        if (genDiff >= 3 && genDiff <= 7) {
            String prefix = DESCENDANT_PREFIX.getOrDefault(genDiff, "玄");
            String suffix = gender == Gender.MALE ? "孙" : "孙女";
            return new RelationshipResult(
                paternal ? prefix + suffix : "外" + prefix + suffix,
                -genDiff, "BLOOD");
        }
        return new RelationshipResult("后代(" + genDiff + "代)", -genDiff, "BLOOD");
    }
}
