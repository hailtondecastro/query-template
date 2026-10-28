# Publish to central

## Register to Publish Via the Central Portal

Create account according instructions on [Register to Publish Via the Central Portal - Documentation](https://central.sonatype.org/register/central-portal/#register-to-publish-via-the-central-portal)

## Register a Namespace

Create namespace according instructions on [Register a Namespace - Documentation](https://central.sonatype.org/register/namespace/#register-a-namespace)

SNAPSHOT's: We need to enable SNAPSHOT's to Namespace. See [Publishing -SNAPSHOT Releases](https://central.sonatype.org/publish/publish-portal-snapshots/)

## Publishing to Maven Central with GitHub Actions

From [Guide to Publishing to Maven Central via the Sonatype Central Portal with GitHub Actions](https://github.com/teamlead/java-maven-sonatype-starter):  

  - Step 1: GitHub Secrets for Automation
    Visit "Actions secrets and variables" page in Github UI (`your_repo/settings/secrets/actions`).  
    Add secrets to your GitHub repository for automated publishing:  
    - `NEXUS_USERNAME` and `NEXUS_PASSWORD`: Generated [from your Sonatype account](https://central.sonatype.com/account) User Token.
    - `GPG_PRIVATE_KEY`: The content of `privkey.asc`.
    - `GPG_PASSPHRASE`: Your GPG key passphrase.
  - Step 2: GPG Key Generation and Distribution
    - Following the [Sonatype GPG requirements](https://central.sonatype.org/publish/requirements/gpg/):  
      - 2.1 Generate a GPG key (`gpg --full-generate-key`) e.g RSA 4096 / No expire.
      - 2.2 Extract the key YOUR_GPG_KEY_ID (`gpg --list-signatures --keyid-format 0xshort`)
      - 2.3 Distribute it (`gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_GPG_KEY_ID`)
      - 2.4 Export the private key (`gpg --armor --export-secret-key <key-id> > privkey.asc`)
        - scripts:          
          ```
          $ # Here I will use a random big passphrase. Important: Store on `GPG_PASSPHRASE` as described at `Step 1` and keep it secure or discard it after that.
          $ gpg --full-generate-key
            gpg (GnuPG) 2.4.8; Copyright (C) 2025 g10 Code GmbH
            This is free software: you are free to change and redistribute it.
            There is NO WARRANTY, to the extent permitted by law.
            
            Please select what kind of key you want:
               (1) RSA and RSA
               (2) DSA and Elgamal
               (3) DSA (sign only)
               (4) RSA (sign only)
               (9) ECC (sign and encrypt) *default*
              (10) ECC (sign only)
              (14) Existing key from card
            Your selection?
            Please select which elliptic curve you want:
               (1) Curve 25519 *default*
               (4) NIST P-384
               (6) Brainpool P-256
            Your selection?
            Please specify how long the key should be valid.
                     0 = key does not expire
                  <n>  = key expires in n days
                  <n>w = key expires in n weeks
                  <n>m = key expires in n months
                  <n>y = key expires in n years
            Key is valid for? (0)
            Key does not expire at all
            Is this correct? (y/N) y
            
            GnuPG needs to construct a user ID to identify your key.
            
            Real name: hailtondecastro
            Email address: hailtondecastro@gmail.com
            Comment: gpg key for sign and publish from `github.com/hailtondecastro/query-template` to `central.sonatype.com`
            You selected this USER-ID:
                "hailtondecastro (gpg key for sign and publish from `github.com/hailtondecastro/query-template` to `central.sonatype.com`) <hailtondecastro@gmail.com>"
            
            Change (N)ame, (C)omment, (E)mail or (O)kay/(Q)uit? O
            We need to generate a lot of random bytes. It is a good idea to perform
            ...
            pub   ed25519 2026-09-11 [SC]
                  F013011567D48C6EB53289FDAEECB716459C8178
            uid                      hailtondecastro (gpg key for sign and publish from `github.com/hailtondecastro/query-template` to `central.sonatype.com`) <hailtondecastro@gmail.com>
            sub   cv25519 2026-09-11 [E]
          $ gpg --list-signatures --keyid-format 0xshort
            gpg: checking the trustdb
            gpg: marginals needed: 3  completes needed: 1  trust model: pgp
            gpg: depth: 0  valid:   1  signed:   0  trust: 0-, 0q, 0n, 0m, 0f, 1u
            ...
            pub   ed25519/0x459C8178 2026-09-11 [SC]
                  F013011567D48C6EB53289FDAEECB716459C8178
            uid           [ultimate] hailtondecastro (gpg key for sign and publish from `github.com/hailtondecastro/query-template` to `central.sonatype.com`) <hailtondecastro@gmail.com>
            sig 3        0x459C8178 2026-09-11  [self-signature]
            sub   cv25519/0x3A7AEC1F 2026-09-11 [E]
            sig          0x459C8178 2026-09-11  [self-signature]
          $ gpg --keyserver keyserver.ubuntu.com --send-keys F013011567D48C6EB53289FDAEECB716459C8178
            gpg: sending key AEECB716459C8178 to hkp://keyserver.ubuntu.com
          $ # Here is needed `random big passphrase`. Important: Store on `GPG_PRIVATE_KEY` as described at `Step 1`.
          $ gpg --armor --export-secret-key F013011567D48C6EB53289FDAEECB716459C8178 > privkey.asc
          ```
      - 2.5 Store the content of `privkey.asc` on `GPG_PRIVATE_KEY` as described at `Step 1`.
      
## Running Github Actions:

  Any commit triggers `build(11)` and `build(21)`.  
  `build(11)` is the build and tests on jdk11, and `build(21)` is the build and tests on jdk21.
  The published artifacts are built with jdk11 for more portability, the jdk21 build is mainly for testing purposes.

  To publish artifacts create release on GitHub, and the corresponding GitHub Actions workflow will handle the publishing process.