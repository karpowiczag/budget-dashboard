FROM gcr.io/distroless/base-debian12:nonroot

WORKDIR /app
COPY target/budget /app/budget

EXPOSE 8080
USER nonroot:nonroot
ENTRYPOINT ["/app/budget"]
