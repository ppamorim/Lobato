package org.ppamorim.lobato.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @hide
 * This annotation indicates that a method on a subclass of View
 * is alllowed to be used with the {@link android.widget.RemoteViews} mechanism.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RemotableViewMethod {
}
