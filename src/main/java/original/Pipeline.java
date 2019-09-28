/*
	Copyright (c) 2018 by imec vzw, Leuven, Belgium. All rights reserverd.
*/

package original;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class Pipeline {
    public static Predicate<SamAlignment> composeFilters(SamHeader header, List<Function<SamHeader, Predicate<SamAlignment>>> filters) {
        if (filters == null) {
            return null;
        } else {
            var alnFilters = new ArrayList<Predicate<SamAlignment>>();
            for (var f : filters) {
                var alnFilter = f.apply(header);
                if (alnFilter != null) {
                    alnFilters.add(alnFilter);
                }
            }
            switch (alnFilters.size()) {
                case 0:
                    return null;
                case 1:
                    return alnFilters.get(0);
                default:
                    return (SamAlignment aln) -> {
                        for (var p : alnFilters) {
                            if (!p.test(aln)) {
                                return false;
                            }
                        }
                        return true;
                    };
            }
        }
    }

    public static Slice effectiveSortingOrder(Slice sortingOrder, SamHeader header, Slice originalSortingOrder) {
        var so = sortingOrder.equals(SamHeader.keep) ? originalSortingOrder : sortingOrder;
        var currentSortingOrder = header.getHD_SO();
        if (so.equals(SamHeader.coordinate) || so.equals(SamHeader.queryname)) {
            if (currentSortingOrder.equals(so)) {
                return SamHeader.keep;
            }
            header.setHD_SO(so);
        } else if (so.equals(SamHeader.unknown) || so.equals(SamHeader.unsorted)) {
            if (!currentSortingOrder.equals(so)) {
                header.setHD_SO(so);
            }
        }
        return so;
    }
}
