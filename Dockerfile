FROM gcr.io/distroless/base-debian12:nonroot

WORKDIR /app
COPY target/budget /app/budget

# Fail closed: the shipped image always runs the production profile, which forces
# OAuth + the Google allowlist on and disables local CSV rebuild. Without this an
# operator who forgets SPRING_PROFILES_ACTIVE=prod would expose private data with
# no authentication. Override at runtime only for deliberate non-prod containers.
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
USER nonroot:nonroot
ENTRYPOINT ["/app/budget"]
