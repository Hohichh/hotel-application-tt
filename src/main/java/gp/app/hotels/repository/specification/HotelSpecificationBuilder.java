package gp.app.hotels.repository.specification;

import gp.app.hotels.model.Hotel;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Function;

public class HotelSpecificationBuilder implements SpecificationBuilder<Hotel> {

    private Specification<Hotel> spec = Specification.where((Specification<Hotel>) null);

    @Override
    public SpecificationBuilder<Hotel> withString(String value, Function<String, Specification<Hotel>> specFunc) {
        if (StringUtils.hasText(value)) {
            spec = spec.and(specFunc.apply(value));
        }
        return this;
    }

    @Override
    public SpecificationBuilder<Hotel> withStringList(List<String> values, Function<String, Specification<Hotel>> specFunc) {
        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    spec = spec.and(specFunc.apply(value));
                }
            }
        }
        return this;
    }

    @Override
    public Specification<Hotel> build() {
        return spec;
    }
}
