Local patches applied on top of the third-party checkouts by
`thirdparty_build.gradle` (task `patchMupdf`, runs after `downloadMupdf`).

Layout mirrors the repository the patch applies to:

    mupdf/*.patch                      -> nativeLibs/mupdf
    mupdf/thirdparty/jbig2dec/*.patch  -> nativeLibs/mupdf/thirdparty/jbig2dec

Patches are applied with `git apply` in name order and skipped if already
applied (`git apply --reverse --check` succeeds). Add a new one as
`NNNN-short-name.patch`.

Upstream submissions with bug reports live in ../../patches.
