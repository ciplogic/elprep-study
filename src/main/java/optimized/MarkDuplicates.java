/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/

package optimized;


import optimized.utils.Slice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

public class MarkDuplicates {

    private static Slice LB = new Slice("LB");
    private static Slice ID = new Slice("ID");

    static void classifyFragment(SamAlignment aln, ConcurrentMap<Fragment, AtomicReference<SamAlignment>> fragments, boolean deterministic) {
        Fragment key = new Fragment(aln.getLIBID(), aln.getREFID(), aln.getAdaptedPos(), aln.isReversed());
        AtomicReference<SamAlignment> best = fragments.putIfAbsent(key, new AtomicReference<>(aln));
        if (best != null) {
            if (aln.isTrueFragment()) {
                int alnScore = aln.getAdaptedScore();
                while (true) {
                    SamAlignment bestAln = best.get();
                    if (bestAln.isTruePair()) {
                        aln.FLAG |= SamAlignment.Duplicate;
                        break;
                    } else {
                        int bestAlnScore = bestAln.getAdaptedScore();
                        if (bestAlnScore > alnScore) {
                            aln.FLAG |= SamAlignment.Duplicate;
                            break;
                        } else if (bestAlnScore == alnScore) {
                            if (deterministic) {
                                if (aln.QNAME.compareTo(bestAln.QNAME) > 0) {
                                    aln.FLAG |= SamAlignment.Duplicate;
                                    break;
                                } else if (best.compareAndSet(bestAln, aln)) {
                                    bestAln.FLAG |= SamAlignment.Duplicate;
                                    break;
                                }
                            } else {
                                aln.FLAG |= SamAlignment.Duplicate;
                                break;
                            }
                        } else if (best.compareAndSet(bestAln, aln)) {
                            bestAln.FLAG |= SamAlignment.Duplicate;
                            break;
                        }
                    }
                }
            } else {
                while (true) {
                    SamAlignment bestAln = best.get();
                    if (bestAln.isTruePair()) {
                        break;
                    } else if (best.compareAndSet(bestAln, aln)) {
                        bestAln.FLAG |= SamAlignment.Duplicate;
                        break;
                    }
                }
            }
        }
    }

    static void classifyPair
            (SamAlignment aln,
             ConcurrentMap<PairFragment, SamAlignment> fragments,
             ConcurrentMap<Pair, AtomicReference<SamAlignmentPair>> pairs,
             boolean deterministic) {
        if (!aln.isTruePair()) {
            return;
        }

        SamAlignment aln1 = aln;
        PairFragment fragmentKey = new PairFragment(aln.getLIBID(), aln.QNAME);
        SamAlignment aln2 = fragments.putIfAbsent(fragmentKey, aln);
        if ((aln2 != null) && fragments.remove(fragmentKey, aln2)) {

            int score = aln1.getAdaptedScore() + aln2.getAdaptedScore();
            int aln1Pos = aln1.getAdaptedPos();
            int aln2Pos = aln2.getAdaptedPos();
            if (aln1Pos > aln2Pos) {
                SamAlignment alnTemp = aln1;
                aln1 = aln2;
                aln2 = alnTemp;
                int temp = aln1Pos;
                aln1Pos = aln2Pos;
                aln2Pos = temp;
            }

            Pair pairKey = new Pair
                    (aln1.getLIBID(),
                            aln1.getREFID(),
                            aln2.getREFID(),
                            aln1Pos,
                            aln2Pos,
                            aln1.isReversed(),
                            aln2.isReversed());

            SamAlignmentPair newPair = new SamAlignmentPair(score, aln1, aln2);

            AtomicReference<SamAlignmentPair> best = pairs.putIfAbsent(pairKey, new AtomicReference<>(newPair));
            if (best != null) {
                while (true) {
                    SamAlignmentPair bestPair = best.get();
                    if (bestPair.score > score) {
                        aln1.FLAG |= SamAlignment.Duplicate;
                        aln2.FLAG |= SamAlignment.Duplicate;
                        break;
                    } else if (bestPair.score == score) {
                        if (deterministic) {
                            if (aln1.QNAME.compareTo(bestPair.aln1.QNAME) > 0) {
                                aln1.FLAG |= SamAlignment.Duplicate;
                                aln2.FLAG |= SamAlignment.Duplicate;
                                break;
                            } else if (best.compareAndSet(bestPair, newPair)) {
                                bestPair.aln1.FLAG |= SamAlignment.Duplicate;
                                bestPair.aln2.FLAG |= SamAlignment.Duplicate;
                                break;
                            }
                        } else {
                            aln1.FLAG |= SamAlignment.Duplicate;
                            aln2.FLAG |= SamAlignment.Duplicate;
                            break;
                        }
                    } else if (best.compareAndSet(bestPair, newPair)) {
                        bestPair.aln1.FLAG |= SamAlignment.Duplicate;
                        bestPair.aln2.FLAG |= SamAlignment.Duplicate;
                        break;
                    }
                }
            }
        }
    }

    public static Function<SamHeader, Predicate<SamAlignment>> markDuplicates(boolean deterministic) {
        return (header) -> {
            ConcurrentHashMap<Fragment, AtomicReference<SamAlignment>> fragments = new ConcurrentHashMap<Fragment, AtomicReference<SamAlignment>>();
            ConcurrentHashMap<PairFragment, SamAlignment> pairsFragments = new ConcurrentHashMap<PairFragment, SamAlignment>();
            ConcurrentHashMap<Pair, AtomicReference<SamAlignmentPair>> pairs = new ConcurrentHashMap<Pair, AtomicReference<SamAlignmentPair>>();
            HashMap<Slice, Slice> lbTable = new HashMap<Slice, Slice>();
            for (Map<Slice, Slice> rgEntry : header.RG) {
                Slice lb = rgEntry.get(LB);
                if (lb != null) {
                    Slice id = rgEntry.get(ID);
                    if (id == null) {
                        throw new RuntimeException("Missing mandatory ID entry in an @RG line in a SAM file header.");
                    }
                    lbTable.put(id, lb);
                }
            }
            return (aln) -> {
                if (aln.flagNotAny(SamAlignment.Unmapped |
                        SamAlignment.Secondary |
                        SamAlignment.Duplicate |
                        SamAlignment.Supplementary)) {
                    aln.adaptAlignment(lbTable);
                    classifyFragment(aln, fragments, deterministic);
                    classifyPair(aln, pairsFragments, pairs, deterministic);
                }
                return true;
            };
        };
    }

    static class Fragment {
        int refid;
        int pos;
        Slice lb;
        boolean reversed;

        Fragment(Slice lb, int refid, int pos, boolean reversed) {
            this.lb = lb;
            this.refid = refid;
            this.pos = pos;
            this.reversed = reversed;
        }

        public boolean equals(Object obj) {
            Fragment that = (Fragment) obj;
            return
                    (this.refid == that.refid) &&
                            (this.pos == that.pos) &&
                            this.lb.equals(that.lb) &&
                            (this.reversed == that.reversed)
                    ;
        }

        public int hashCode() {
            return lb.hashCode() ^ Integer.hashCode(refid) ^ Integer.hashCode(pos) ^ Boolean.hashCode(reversed);
        }
    }

    static class PairFragment {
        Slice lb;
        Slice qname;

        PairFragment(Slice lb, Slice qname) {
            this.lb = lb;
            this.qname = qname;
        }

        public boolean equals(Object obj) {
            PairFragment that = (PairFragment) obj;
            return this.lb.equals(that.lb) &&
                    this.qname.equals(that.qname);
        }

        public int hashCode() {
            return lb.hashCode() ^ qname.hashCode();
        }
    }

    static class Pair {
        int refid1, refid2;
        int pos1, pos2;
        Slice lb;
        boolean reversed1, reversed2;

        Pair(Slice lb, int refid1, int refid2, int pos1, int pos2, boolean reversed1, boolean reversed2) {
            this.lb = lb;
            this.refid1 = refid1;
            this.refid2 = refid2;
            this.pos1 = pos1;
            this.pos2 = pos2;
            this.reversed1 = reversed1;
            this.reversed2 = reversed2;
        }

        public boolean equals(Object obj) {
            Pair that = (Pair) obj;
            return this.lb.equals(that.lb) &&
                    (this.refid1 == that.refid1) &&
                    (this.refid2 == that.refid2) &&
                    (this.pos1 == that.pos1) &&
                    (this.pos2 == that.pos2) &&
                    (this.reversed1 == that.reversed1) &&
                    (this.reversed2 == that.reversed2);
        }

        public int hashCode() {
            return lb.hashCode() ^
                    Integer.hashCode(refid1) ^
                    Integer.hashCode(refid2) ^
                    Long.hashCode((((long) pos1) << 23) ^ (long) pos2) ^
                    Boolean.hashCode(reversed1) ^
                    Boolean.hashCode(reversed2);
        }
    }

    static class SamAlignmentPair {
        int score;
        SamAlignment aln1, aln2;

        SamAlignmentPair(int score, SamAlignment aln1, SamAlignment aln2) {
            this.score = score;
            this.aln1 = aln1;
            this.aln2 = aln2;
        }
    }
}
