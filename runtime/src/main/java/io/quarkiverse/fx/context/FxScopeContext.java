package io.quarkiverse.fx.context;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;

public class FxScopeContext implements InjectableContext {

    private final FxScopeContextState state = new FxScopeContextState();
    private final Map<Class<?>, FxScopeInstance<?>> beansMap = new HashMap<>();

    //    // TODO
    //    public Object getInstance(final Class<?> cl) {
    //        FxScopeInstance<?> fxScopeInstance = this.beansMap.get(cl);
    //        if (fxScopeInstance != null) {
    //            return fxScopeInstance.instance;
    //        }
    //
    //        return null;
    //    }

    @Override
    public ContextState getState() {
        // TODO
        //        return null;
        return this.state;

        //        return this.beansMap.values().stream()
        //          .collect(Collectors.toMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
    }

    @Override
    public void destroy() {
        System.out.println("destroy");
    }

    @Override
    public void destroy(final Contextual<?> contextual) {
        System.out.println("destroy " + contextual);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return FxScoped.class;
    }

    @Override
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        Bean<T> bean = (Bean<T>) contextual;
        if (this.beansMap.containsKey(bean.getBeanClass())) {
            return (T) this.beansMap.get(bean.getBeanClass()).instance;
        } else {
            T instance = contextual.create(creationalContext);
            FxScopeInstance<T> customInstance = new FxScopeInstance<>(bean, creationalContext, instance);
            this.beansMap.put(customInstance.bean.getBeanClass(), customInstance);

            return instance;
        }
    }

    @Override
    public <T> T get(final Contextual<T> contextual) {
        Bean<T> bean = (Bean<T>) contextual;
        if (this.beansMap.containsKey(bean.getBeanClass())) {
            return (T) this.beansMap.get(bean.getBeanClass()).instance;
        } else {
            return null;
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    private record FxScopeInstance<T>(
            Bean<T> bean,
            CreationalContext<T> ctx,
            T instance) {
    }

    private static class FxScopeContextState implements ContextState {

        private final Map<Contextual<?>, ContextInstanceHandle<?>> mapBeanToInstanceHandle = new HashMap<>();

        //        TransactionContextState(Transaction transaction) {
        //            try {
        //                transaction.registerSynchronization(this);
        //            } catch (RollbackException | SystemException e) {
        //                throw new RuntimeException("Cannot register synchronization", e);
        //            }
        //        }

        /**
         * Put the contextual bean and its handle to the container.
         *
         * @param bean bean to be added
         * @param handle handle for the bean which incorporates the bean, contextual instance and the context
         */
        <T> void put(final Contextual<T> bean, final ContextInstanceHandle<T> handle) {
            this.mapBeanToInstanceHandle.put(bean, handle);
        }

        /**
         * Remove the bean from the container.
         *
         * @param bean contextual bean instance
         */
        <T> void remove(final Contextual<T> bean) {
            ContextInstanceHandle<?> instance = this.mapBeanToInstanceHandle.remove(bean);
            if (instance != null) {
                instance.destroy();
            }
        }

        /**
         * Retrieve the bean saved in the container.
         *
         * @param bean retrieving the bean from the container, otherwise {@code null} is returned
         */
        <T> ContextInstanceHandle<T> get(final Contextual<T> bean) {
            return (ContextInstanceHandle<T>) this.mapBeanToInstanceHandle.get(bean);
        }

        /**
         * Destroying all the beans in the container and clearing the container.
         */
        void destroy() {
            for (ContextInstanceHandle<?> handle : this.mapBeanToInstanceHandle.values()) {
                handle.destroy();
            }
            this.mapBeanToInstanceHandle.clear();
        }

        /**
         * Method required by the {@link io.quarkus.arc.InjectableContext.ContextState} interface
         * which is then used to get state of the scope in method {@link InjectableContext#getState()}
         *
         * @return Map of context bean and the bean instances which are available in the container
         */
        @Override
        public Map<InjectableBean<?>, Object> getContextualInstances() {
            return this.mapBeanToInstanceHandle.values().stream()
                    .collect(Collectors.toMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
        }
    }
}
