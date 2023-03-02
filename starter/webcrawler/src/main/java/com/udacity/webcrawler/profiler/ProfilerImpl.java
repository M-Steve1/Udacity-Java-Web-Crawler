package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  private boolean isClassProfiled(Class<?> aClass) {
    Method[] methods = aClass.getDeclaredMethods();
    if (methods.length == 0) return false;
    for (Method method : methods) {
      if (method.getAnnotation(Profiled.class) != null) {
        return true;
      }
    }
    return false;
  }
  @Override
  public <T> T wrap(Class<T> klass, T delegate) throws IllegalArgumentException {
    Objects.requireNonNull(klass);
    boolean isProfiled = isClassProfiled(klass);
    if (!isProfiled) {
      throw new IllegalArgumentException(
              "Does not contain Profile annotation"
      );
    }

    InvocationHandler handler = new ProfilingMethodInterceptor(
            clock,
            delegate,
            state
    );

    ClassLoader loader = klass.getClassLoader();

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.

    @SuppressWarnings("unchecked")
    T proxy = (T) Proxy.newProxyInstance(
            loader,
            new Class[]{klass},
            handler
    );

    return proxy;
  }

  @Override
  public void writeData(Path path) {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.
    try (BufferedWriter out = new BufferedWriter(new FileWriter(path.toString(), true))){
      writeData(out);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
