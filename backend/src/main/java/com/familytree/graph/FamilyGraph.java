package com.familytree.graph;

import com.familytree.model.Person;
import com.familytree.model.Relationship;
import com.familytree.model.enums.Gender;
import com.familytree.model.enums.RelationshipType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 内存中的家族图结构。
 * 从数据库加载所有人物和关系，构建有向图。
 *
 * 边类型：
 *   PARENT → CHILD（亲子：从父亲/母亲指向孩子）
 *   SPOUSE（配偶：双向无向）
 */
public class FamilyGraph {

    // 所有人物 ID → Person
    private final Map<Long, Person> personMap = new HashMap<>();

    // 亲子边：parentId → [childId, ...]
    private final Map<Long, Set<Long>> parentToChildren = new HashMap<>();

    // 反向亲子边：childId → [parentId, ...]（通常一个孩子有0~2个父母）
    private final Map<Long, Set<Long>> childToParents = new HashMap<>();

    // 配偶边：personId → spouseId（双向）
    private final Map<Long, Long> spouseMap = new HashMap<>();

    // 每个孩子的父母信息（区分父亲和母亲）
    private final Map<Long, Long> fatherOf = new HashMap<>();
    private final Map<Long, Long> motherOf = new HashMap<>();

    /**
     * 从数据库数据构建图
     */
    public static FamilyGraph buildFrom(List<Person> persons, List<Relationship> relationships) {
        FamilyGraph graph = new FamilyGraph();

        // 添加所有人物
        for (Person p : persons) {
            graph.personMap.put(p.getId(), p);
        }

        // 处理关系
        for (Relationship r : relationships) {
            Long p1 = r.getPerson1().getId();
            Long p2 = r.getPerson2().getId();

            switch (r.getType()) {
                case FATHER:
                case MOTHER:
                    // p1 是 p2 的父亲/母亲 → parent → child
                    graph.addParentChild(p1, p2);
                    if (r.getType() == RelationshipType.FATHER) {
                        graph.fatherOf.put(p2, p1);
                    } else {
                        graph.motherOf.put(p2, p1);
                    }
                    break;
                case SON:
                case DAUGHTER:
                    // p1 是 p2 的儿子/女儿 → p2 是 p1 的父亲/母亲
                    graph.addParentChild(p2, p1);
                    break;
                case HUSBAND:
                case WIFE:
                    // 配偶关系
                    graph.spouseMap.put(p1, p2);
                    graph.spouseMap.put(p2, p1);
                    break;
                case CUSTOM:
                    // 自定义关系不参与图计算
                    break;
            }
        }

        return graph;
    }

    private void addParentChild(Long parentId, Long childId) {
        parentToChildren.computeIfAbsent(parentId, k -> new HashSet<>()).add(childId);
        childToParents.computeIfAbsent(childId, k -> new HashSet<>()).add(parentId);
    }

    // ===== 查询方法 =====

    public Person getPerson(Long id) {
        return personMap.get(id);
    }

    public Set<Long> getChildren(Long personId) {
        return parentToChildren.getOrDefault(personId, Collections.emptySet());
    }

    public Set<Long> getParents(Long personId) {
        return childToParents.getOrDefault(personId, Collections.emptySet());
    }

    public Long getSpouse(Long personId) {
        return spouseMap.get(personId);
    }

    public Long getFather(Long personId) {
        return fatherOf.get(personId);
    }

    public Long getMother(Long personId) {
        return motherOf.get(personId);
    }

    public boolean hasPerson(Long id) {
        return personMap.containsKey(id);
    }

    public Collection<Person> getAllPersons() {
        return personMap.values();
    }

    public Set<Long> getAllPersonIds() {
        return personMap.keySet();
    }

    public Gender getGender(Long personId) {
        Person p = personMap.get(personId);
        return p != null ? p.getGender() : null;
    }

    /**
     * 获取某人的所有祖先（BFS 向上查找，最多 maxGenerations 代）
     * 返回 ancestorId → AncestorInfo（包含深度和路径）
     */
    public Map<Long, AncestorInfo> findAllAncestors(Long personId, int maxGenerations) {
        Map<Long, AncestorInfo> ancestors = new HashMap<>();

        // BFS 队列：[(personId, depth, isPaternal)]
        Queue<Object[]> queue = new LinkedList<>();
        // 初始：从 person 自己开始（记录 lineage）
        // 对于祖先追溯，从 person 的父母开始
        Set<Long> parents = getParents(personId);
        for (Long parentId : parents) {
            boolean isPaternal = isFather(personId, parentId);
            AncestorInfo info = new AncestorInfo(parentId, 1, isPaternal);
            ancestors.put(parentId, info);
            queue.add(new Object[]{parentId, 1, isPaternal});
        }

        while (!queue.isEmpty()) {
            Object[] current = queue.poll();
            Long currentId = (Long) current[0];
            int depth = (int) current[1];
            boolean isPaternal = (boolean) current[2];

            if (depth >= maxGenerations) continue;

            Set<Long> currentParents = getParents(currentId);
            for (Long grandParentId : currentParents) {
                // 父系/母系由第一步决定：从父亲上去则为父系，从母亲上去则为母系
                // 后续祖先继承该认定，不再重新判断
                boolean newIsPaternal = isPaternal;
                if (!ancestors.containsKey(grandParentId) || ancestors.get(grandParentId).depth > depth + 1) {
                    AncestorInfo info = new AncestorInfo(grandParentId, depth + 1, newIsPaternal);
                    ancestors.put(grandParentId, info);
                    queue.add(new Object[]{grandParentId, depth + 1, newIsPaternal});
                }
            }
        }

        return ancestors;
    }

    /**
     * 判断 parentId 是否是 childId 的父亲
     */
    private boolean isFather(Long childId, Long parentId) {
        Long father = fatherOf.get(childId);
        return father != null && father.equals(parentId);
    }

    /**
     * 获取某人的所有后代（BFS 向下查找，最多 maxGenerations 代）
     */
    public Map<Long, Integer> findAllDescendants(Long personId, int maxGenerations) {
        Map<Long, Integer> descendants = new HashMap<>();
        Queue<Object[]> queue = new LinkedList<>();

        Set<Long> children = getChildren(personId);
        for (Long childId : children) {
            descendants.put(childId, 1);
            queue.add(new Object[]{childId, 1});
        }

        while (!queue.isEmpty()) {
            Object[] current = queue.poll();
            Long currentId = (Long) current[0];
            int depth = (int) current[1];

            if (depth >= maxGenerations) continue;

            Set<Long> grandchildren = getChildren(currentId);
            for (Long grandchildId : grandchildren) {
                if (!descendants.containsKey(grandchildId)) {
                    descendants.put(grandchildId, depth + 1);
                    queue.add(new Object[]{grandchildId, depth + 1});
                }
            }
        }

        return descendants;
    }

    /**
     * 查找从 person1 到 person2 的完整路径。
     * 路径由一个个段组成，每个段可以是 UP（向上=到父母）、DOWN（向下=到孩子）、SPOUSE（配偶）。
     */
    public RelationshipPath findPath(Long personA, Long personB, int maxGenerations) {
        if (personA.equals(personB)) return null;

        // 1. 获取 A 的所有祖先
        Map<Long, AncestorInfo> ancestorsA = findAllAncestors(personA, maxGenerations);
        Map<Long, AncestorInfo> ancestorsB = findAllAncestors(personB, maxGenerations);

        // 2. 检查直系关系
        if (ancestorsA.containsKey(personB)) {
            // personB 是 personA 的祖先
            return buildStraightPath(personA, personB, ancestorsA, true);
        }
        if (ancestorsB.containsKey(personA)) {
            // personA 是 personB 的祖先
            return buildStraightPath(personA, personB, ancestorsB, false);
        }

        // 3. 查找最近共同祖先（NCA）
        Long ncaId = findNearestCommonAncestor(ancestorsA, ancestorsB);
        if (ncaId != null) {
            return buildPathViaNCA(personA, personB, ncaId, ancestorsA, ancestorsB);
        }

        // 4. 尝试通过配偶查找
        Long spouseA = getSpouse(personA);
        Long spouseB = getSpouse(personB);

        // 如果 A 的配偶与 B 有血缘关系
        if (spouseA != null) {
            Map<Long, AncestorInfo> ancestorsSpouseA = findAllAncestors(spouseA, maxGenerations);
            if (ancestorsSpouseA.containsKey(personB)) {
                return buildPathViaSpouse(personA, spouseA, personB, ancestorsSpouseA, null, maxGenerations);
            }
            Long nca2 = findNearestCommonAncestor(ancestorsSpouseA, ancestorsB);
            if (nca2 != null) {
                return buildPathViaSpouse(personA, spouseA, personB, null, ancestorsB, maxGenerations);
            }
        }

        if (spouseB != null) {
            Map<Long, AncestorInfo> ancestorsSpouseB = findAllAncestors(spouseB, maxGenerations);
            if (ancestorsSpouseB.containsKey(personA)) {
                return buildPathViaSpouse(personB, spouseB, personA, ancestorsSpouseB, null, maxGenerations);
            }
            Long nca3 = findNearestCommonAncestor(ancestorsA, ancestorsSpouseB);
            if (nca3 != null) {
                return buildPathViaSpouse2(personA, personB, spouseB, ancestorsA, ancestorsSpouseB);
            }
        }

        // 5. 检查 A 的祖先的配偶是否是 B（如：父亲妻子=母亲）
        for (Map.Entry<Long, AncestorInfo> entry : ancestorsA.entrySet()) {
            Long ancestorId = entry.getKey();
            Long spouseOfAncestor = getSpouse(ancestorId);
            if (spouseOfAncestor != null && spouseOfAncestor.equals(personB)) {
                return buildPathViaAncestorSpouse(personA, personB, ancestorId, entry.getValue());
            }
        }
        // 对称检查：A 是否是 B 的某个祖先的配偶
        for (Map.Entry<Long, AncestorInfo> entry : ancestorsB.entrySet()) {
            Long ancestorId = entry.getKey();
            Long spouseOfAncestor = getSpouse(ancestorId);
            if (spouseOfAncestor != null && spouseOfAncestor.equals(personA)) {
                return buildPathViaAncestorSpouse(personB, personA, ancestorId, entry.getValue());
            }
        }

        // 6. 通过祖先的配偶扩展查找（A→祖先→祖先配偶→配偶的亲属=B）
        // 如：小刚→父亲小明→妻子小红→小红母亲=姥姥
        for (Map.Entry<Long, AncestorInfo> aEntry : ancestorsA.entrySet()) {
            Long ancestorId = aEntry.getKey();
            Long spouseOfAncestor = getSpouse(ancestorId);
            if (spouseOfAncestor == null) continue;

            Map<Long, AncestorInfo> ancestorsOfSpouse = findAllAncestors(spouseOfAncestor, maxGenerations);
            if (ancestorsOfSpouse.containsKey(personB)) {
                return buildPathViaSpouseExtended(personA, personB, aEntry.getValue(), ancestorsOfSpouse.get(personB));
            }
            Map<Long, Integer> descendantsOfSpouse = findAllDescendants(spouseOfAncestor, maxGenerations);
            if (descendantsOfSpouse.containsKey(personB)) {
                return buildPathViaSpouseExtendedDown(personA, personB, aEntry.getValue(), descendantsOfSpouse.get(personB));
            }
        }
        for (Map.Entry<Long, AncestorInfo> bEntry : ancestorsB.entrySet()) {
            Long ancestorId = bEntry.getKey();
            Long spouseOfAncestor = getSpouse(ancestorId);
            if (spouseOfAncestor == null) continue;

            Map<Long, AncestorInfo> ancestorsOfSpouse = findAllAncestors(spouseOfAncestor, maxGenerations);
            if (ancestorsOfSpouse.containsKey(personA)) {
                return buildPathViaSpouseExtended(personB, personA, bEntry.getValue(), ancestorsOfSpouse.get(personA));
            }
            Map<Long, Integer> descendantsOfSpouse = findAllDescendants(spouseOfAncestor, maxGenerations);
            if (descendantsOfSpouse.containsKey(personA)) {
                return buildPathViaSpouseExtendedDown(personB, personA, bEntry.getValue(), descendantsOfSpouse.get(personA));
            }
        }

        // 没有找到路径
        return null;
    }

    private RelationshipPath buildPathViaAncestorSpouse(Long person, Long targetSpouse, Long ancestor, AncestorInfo ancestorInfo) {
        RelationshipPath path = new RelationshipPath();
        path.personA = person;
        path.personB = targetSpouse;
        path.hasSpouseEdge = true;
        path.spouseConnectorId = ancestor;
        path.generationDiff = ancestorInfo.depth;
        path.isPaternal = ancestorInfo.isPaternal;
        return path;
    }

    /**
     * 通过祖先的配偶扩展路径：A→祖先→祖先配偶→配偶的亲属(B)
     * ancestorUp: A到其祖先的AncestorInfo
     * spouseRel: 配偶到B的AncestorInfo（B在配偶的祖先链上）
     * 总代差 = A到祖先的深度 + 配偶到B的深度
     */
    private RelationshipPath buildPathViaSpouseExtended(Long a, Long b, AncestorInfo ancestorUp, AncestorInfo spouseRel) {
        RelationshipPath path = new RelationshipPath();
        path.personA = a;
        path.personB = b;
        path.hasSpouseEdge = true;
        path.generationDiff = ancestorUp.depth + spouseRel.depth;
        path.isPaternal = ancestorUp.isPaternal && spouseRel.isPaternal;
        return path;
    }

    private RelationshipPath buildPathViaSpouseExtendedDown(Long a, Long b, AncestorInfo ancestorUp, int downDepth) {
        RelationshipPath path = new RelationshipPath();
        path.personA = a;
        path.personB = b;
        path.hasSpouseEdge = true;
        path.generationDiff = ancestorUp.depth - downDepth;
        path.isPaternal = ancestorUp.isPaternal;
        return path;
    }

    private Long findNearestCommonAncestor(Map<Long, AncestorInfo> ancestorsA, Map<Long, AncestorInfo> ancestorsB) {
        Long bestNca = null;
        int bestDepth = Integer.MAX_VALUE;
        boolean bestIsPaternal = true;

        for (Map.Entry<Long, AncestorInfo> entry : ancestorsA.entrySet()) {
            Long ancestorId = entry.getKey();
            AncestorInfo infoA = entry.getValue();

            if (ancestorsB.containsKey(ancestorId)) {
                AncestorInfo infoB = ancestorsB.get(ancestorId);
                int combinedDepth = infoA.depth + infoB.depth;

                if (bestNca == null || combinedDepth < bestDepth ||
                    (combinedDepth == bestDepth && infoA.isPaternal && !bestIsPaternal)) {
                    bestNca = ancestorId;
                    bestDepth = combinedDepth;
                    bestIsPaternal = infoA.isPaternal;
                }
            }
        }

        return bestNca;
    }

    private RelationshipPath buildStraightPath(Long personA, Long personB,
                                                Map<Long, AncestorInfo> ancestors, boolean bIsAncestor) {
        RelationshipPath path = new RelationshipPath();

        if (bIsAncestor) {
            // personB 是 personA 的祖先
            AncestorInfo info = ancestors.get(personB);
            path.generationDiff = info.depth;
            path.personA = personA;
            path.personB = personB;
            path.isPaternal = info.isPaternal;
            path.addUpSegment(info.depth, personA, personB);
        } else {
            // personA 是 personB 的祖先（反过来查找）
            Map<Long, AncestorInfo> ancestorsFromB = findAllAncestors(personB, 7);
            AncestorInfo info = ancestorsFromB.get(personA);
            path.generationDiff = -info.depth;
            path.personA = personA;
            path.personB = personB;
            path.isPaternal = info.isPaternal;
            path.addDownSegment(info.depth, personA, personB);
        }

        return path;
    }

    private RelationshipPath buildPathViaNCA(Long personA, Long personB, Long ncaId,
                                              Map<Long, AncestorInfo> ancestorsA,
                                              Map<Long, AncestorInfo> ancestorsB) {
        RelationshipPath path = new RelationshipPath();
        path.personA = personA;
        path.personB = personB;

        AncestorInfo infoA = ancestorsA.get(ncaId);
        AncestorInfo infoB = ancestorsB.get(ncaId);

        path.generationDiff = infoA.depth - infoB.depth;
        path.ncaId = ncaId;
        path.isPaternal = infoA.isPaternal && infoB.isPaternal;

        // 构建上行段（A 到 NCA）
        path.addUpSegment(infoA.depth, personA, ncaId);

        // 构建下行段（NCA 到 B）
        path.addDownSegment(infoB.depth, ncaId, personB);

        return path;
    }

    private RelationshipPath buildPathViaSpouse(Long personA, Long spouseA, Long personB,
                                                 Map<Long, AncestorInfo> ancestorsSpouseA,
                                                 Map<Long, AncestorInfo> ancestorsB,
                                                 int maxGenerations) {
        RelationshipPath path = new RelationshipPath();
        path.personA = personA;
        path.personB = personB;
        path.hasSpouseEdge = true;

        if (ancestorsSpouseA != null && ancestorsSpouseA.containsKey(personB)) {
            // A 的配偶的血缘到达 B
            AncestorInfo info = ancestorsSpouseA.get(personB);
            path.generationDiff = 1 + info.depth; // A→spouse + spouse→...→B
            path.spouseConnectorId = spouseA;
        } else if (ancestorsB != null) {
            Long nca = findNearestCommonAncestor(ancestorsSpouseA, ancestorsB);
            if (nca != null) {
                AncestorInfo infoSpouse = ancestorsSpouseA.get(nca);
                AncestorInfo infoB = ancestorsB.get(nca);
                path.generationDiff = (1 + infoSpouse.depth) - infoB.depth;
                path.ncaId = nca;
                path.spouseConnectorId = spouseA;
                path.isPaternal = infoSpouse.isPaternal && infoB.isPaternal;
            }
        }

        return path;
    }

    private RelationshipPath buildPathViaSpouse2(Long personA, Long personB, Long spouseB,
                                                  Map<Long, AncestorInfo> ancestorsA,
                                                  Map<Long, AncestorInfo> ancestorsSpouseB) {
        RelationshipPath path = new RelationshipPath();
        path.personA = personA;
        path.personB = personB;
        path.hasSpouseEdge = true;

        Long nca = findNearestCommonAncestor(ancestorsA, ancestorsSpouseB);
        if (nca != null) {
            AncestorInfo infoA = ancestorsA.get(nca);
            AncestorInfo infoSpouseB = ancestorsSpouseB.get(nca);
            path.generationDiff = infoA.depth - (1 + infoSpouseB.depth);
            path.ncaId = nca;
            path.spouseConnectorId = spouseB;
            path.isPaternal = infoA.isPaternal && infoSpouseB.isPaternal;
        }

        return path;
    }

    // ==== 辅助数据结构 ====

    public static class AncestorInfo {
        public Long ancestorId;
        public int depth;          // 距离起始人的代数
        public boolean isPaternal; // 是否通过父系

        public AncestorInfo(Long ancestorId, int depth, boolean isPaternal) {
            this.ancestorId = ancestorId;
            this.depth = depth;
            this.isPaternal = isPaternal;
        }
    }

    public static class RelationshipPath {
        public Long personA;
        public Long personB;
        public Long ncaId;                 // 最近共同祖先（如果有）
        public Long spouseConnectorId;     // 配偶连接点（如果通过配偶）
        public int generationDiff;         // 代差：正=B是长辈，负=B是晚辈，0=同辈
        public boolean isPaternal;         // 是否父系
        public boolean hasSpouseEdge;      // 路径中是否有配偶边
        public int upSteps;                // 从A向上的步数
        public int downSteps;              // 从NCA向下的步数（到B）

        public void addUpSegment(int steps, Long from, Long to) {
            this.upSteps = steps;
        }

        public void addDownSegment(int steps, Long from, Long to) {
            this.downSteps = steps;
        }
    }
}
