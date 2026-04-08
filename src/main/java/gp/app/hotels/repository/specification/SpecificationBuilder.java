package gp.app.hotels.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.function.Function;

public interface SpecificationBuilder<T> {

    SpecificationBuilder<T> withString(String value, Function<String, Specification<T>> specificationFunction);

    SpecificationBuilder<T> withStringList(List<String> values, Function<String, Specification<T>> specificationFunction);

    Specification<T> build();
}
