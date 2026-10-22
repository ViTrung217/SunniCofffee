package vn.sunicoffee.service;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sunicoffee.domain.Product;
import vn.sunicoffee.repository.ProductRepository;

/** Accent-independent matching for the local catalogue, with identical H2/MySQL semantics. */
@Service
@Transactional(readOnly=true)
public class ProductSearchService {
    private final ProductRepository products;
    public ProductSearchService(ProductRepository products) { this.products=products; }
    private record Match(long id,int score) {}
    public Page<Product> search(String query,Long category,int page) {
        String q=query==null?"":query.strip();
        if(q.length()>100) q=q.substring(0,100);
        var pageable=PageRequest.of(Math.max(0,page),12,Sort.by("id"));
        if(q.isEmpty()) return products.search("",category,false,pageable);
        String normalized=normalize(q).replaceAll("\\bphee\\b","phe");
        if(normalized.isEmpty()) return Page.empty(pageable);
        var words=Arrays.stream(normalized.split(" ")).distinct().toList();
        // Fetch only searchable metadata, not descriptions, images or complete entities.
        var ranked=products.searchCandidates(category).stream().map(p->{
            String name=normalize(p.getName()), sku=normalize(p.getSku()), group=normalize(p.getCategoryName());
            String haystack=name+" "+sku+" "+group;
            if(!words.stream().allMatch(haystack::contains)) return null;
            int score=(sku.equals(normalized)?1000:0)+(name.equals(normalized)?500:0)
                +(name.contains(normalized)?100:0)+(group.contains(normalized)?50:0)
                +(int)words.stream().filter(name::contains).count();
            return new Match(p.getId(),score);
        }).filter(Objects::nonNull).sorted(Comparator.comparingInt(Match::score).reversed().thenComparingLong(Match::id)).toList();
        long offset=pageable.getOffset();
        if(offset>=ranked.size()) return new PageImpl<>(List.of(),pageable,ranked.size());
        var ids=ranked.subList((int)offset,Math.min((int)offset+pageable.getPageSize(),ranked.size())).stream().map(Match::id).toList();
        var fetched=products.findAllById(ids).stream().collect(Collectors.toMap(Product::getId,Function.identity()));
        return new PageImpl<>(ids.stream().map(fetched::get).filter(Objects::nonNull).toList(),pageable,ranked.size());
    }
    static String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT).replace('đ','d'),Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","").replaceAll("[^\\p{L}\\p{N}]+"," ").strip().replaceAll("\\s+"," ");
    }
}
