package fr.sevensins;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class SevenSins extends JavaPlugin implements Listener, TabExecutor {

    // ============ ARMES ============
    enum Weapon {
        LOSTVAYNE("lostvayne", "Lostvayne", Material.IRON_SWORD, 1001, NamedTextColor.GREEN, "Meliodas",
                "L'épée courte de Meliodas possède la capacité Jitsuzō Bunshin (Création de Clones) : jusqu'à quatre clones physiques, la force étant divisée entre eux. Chaque clone peut utiliser le Contre Total pour renvoyer plusieurs attaques magiques sous différents angles.",
                "Clic droit : créer 4 clones (30s)", "Sneak : Contre Total (renvoie x2 les dégâts)"),
        CHASTIEFOL("chastiefol", "Chastiefol", Material.TRIDENT, 1002, NamedTextColor.GOLD, "King",
                "La lance spirituelle de King, l'arme la plus polyvalente de la série, activée par la manipulation de l'Arbre Divin. Elle possède plusieurs formes : Chastiefol, Guardian, Fossilization, Sunflower, Increase, Yggdra Cloth, Luminosity et Pollen Garden.",
                "Lance vanilla : mêlée + lancer (Loyauté)", "Clic gauche (air) : forme active  |  Sneak + clic droit : changer de forme"),
        GIDEON("gideon", "Gideon", Material.MACE, 1003, NamedTextColor.DARK_GREEN, "Diane",
                "Le marteau de guerre géant de Diane, catalyseur ultime de sa magie Creation. Il décuple sa maîtrise de la terre (montagnes, golems, séismes), attire la foudre et dissipe dans le sol les attaques magiques avec Lightning Disruption.",
                "Clic droit : séisme  |  Sneak + clic droit : golem de terre", "Clic gauche : attirer la foudre  |  Sneak : Lightning Disruption"),
        RHITTA("rhitta", "Rhitta", Material.NETHERITE_AXE, 1004, NamedTextColor.YELLOW, "Escanor",
                "La hache divine d'Escanor possède la capacité Charge and Fire : elle absorbe passivement la chaleur solaire d'Escanor, puis libère toute la puissance accumulée en une explosion solaire. Escanor peut aussi l'appeler à distance vers sa main.",
                "Hache vanilla (désactive les boucliers)", "En main : charge solaire  |  Clic droit : explosion  |  F : rappel"),
        HERRITT("herritt", "Herritt", Material.BOW, 1005, NamedTextColor.LIGHT_PURPLE, "Gowther",
                "Les doubles arcs d'énergie lumineuse de Gowther amplifient sa magie Invasion : portée de plusieurs kilomètres, cibles multiples. Les flèches de lumière traversent les défenses physiques pour atteindre l'esprit des ennemis, réécrire leurs souvenirs ou les piéger dans des illusions éternelles.",
                "Arc vanilla : tire 2 flèches de lumière à la fois", "Sneak + clic gauche : illusion éternelle de zone"),
        COURECHOUSE("courechouse", "Courechouse", Material.BLAZE_ROD, 1006, NamedTextColor.RED, "Ban",
                "Le nunchaku à quatre sections de Ban décuple sa vitesse, son agilité et sa allonge. En synergie totale avec la magie Snatch, il permet la Super Concentration : voler la force et la vitesse de milliers d'ennemis simultanément.",
                "Nunchaku : attaque très rapide, dégâts en combo", "Clic droit : Super Concentration (vol de force)"),
        ALDAN("aldan", "Aldan", Material.HEART_OF_THE_SEA, 1007, NamedTextColor.AQUA, "Merlin",
                "La sphère de cristal magique de Merlin, réceptacle de sa magie infinie (Infinity). Elle projette des sorts à distance et sert de clairvoyance. Si le corps de Merlin est détruit, son âme peut se transférer dans la sphère pour rester en vie.",
                "Clic droit : clairvoyance  |  Sneak + clic droit : sort arcanique", "Passif : sauvegarde de l'âme (5 min)");

        final String id, display, owner, desc; final Material mat; final int cmd; final TextColor color; final String[] abilities;
        Weapon(String id, String display, Material mat, int cmd, TextColor color, String owner, String desc, String... abilities) {
            this.id = id; this.display = display; this.mat = mat; this.cmd = cmd; this.color = color; this.owner = owner; this.desc = desc; this.abilities = abilities;
        }
        static Weapon byId(String id) { for (Weapon w : values()) if (w.id.equalsIgnoreCase(id)) return w; return null; }
    }

    static final String[] FORMS = {"Chastiefol", "Guardian", "Fossilization", "Sunflower", "Increase", "Yggdra Cloth", "Luminosity", "Pollen Garden"};

    NamespacedKey KEY;
    final Map<String, Long> cds = new HashMap<>();
    final Map<UUID, Integer> forms = new HashMap<>();
    final Map<UUID, Integer> charge = new HashMap<>();
    final Map<UUID, List<ArmorStand>> clones = new HashMap<>();
    final Map<UUID, UUID> cloneOwner = new HashMap<>();
    final Set<UUID> minions = new HashSet<>();
    final Map<UUID, UUID> rhittaDrop = new HashMap<>();
    final Map<UUID, long[]> combo = new HashMap<>();
    boolean busy = false;

    @Override
    public void onEnable() {
        KEY = new NamespacedKey(this, "treasure");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("sin").setExecutor(this);
        getCommand("sin").setTabCompleter(this);
        // Rhitta : charge solaire
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (weaponOf(p.getInventory().getItemInMainHand()) == Weapon.RHITTA) {
                    int c = Math.min(100, charge.getOrDefault(p.getUniqueId(), 0) + 5);
                    charge.put(p.getUniqueId(), c);
                    p.sendActionBar(Component.text("☀ Charge solaire : " + c + "%", NamedTextColor.YELLOW));
                    p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 3 + c / 10, 0.4, 0.6, 0.4, 0.01);
                }
            }
        }, 20, 20);
        // Clones de Lostvayne qui attaquent les monstres proches
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Map.Entry<UUID, List<ArmorStand>> en : clones.entrySet()) {
                int n = en.getValue().size();
                Player owner = Bukkit.getPlayer(en.getKey());
                for (ArmorStand c : en.getValue()) {
                    if (!c.isValid()) continue;
                    for (Entity near : c.getNearbyEntities(4, 2, 4)) {
                        if (near instanceof Monster m) {
                            m.damage(8.0 / (n + 0), owner);
                            c.swingMainHand();
                            c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, m.getLocation().add(0, 1, 0), 1);
                            break;
                        }
                    }
                }
            }
        }, 20, 20);
    }

    @Override
    public void onDisable() {
        for (UUID id : new ArrayList<>(clones.keySet())) removeClones(id);
        for (UUID id : minions) { Entity e = Bukkit.getEntity(id); if (e != null) e.remove(); }
    }

    // ============ ITEMS ============
    ItemStack create(Weapon w) {
        ItemStack it = new ItemStack(w.mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text(w.display, w.color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Trésor Sacré de " + w.owner, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.addAll(wrap(w.desc, 42, NamedTextColor.WHITE));
        lore.add(Component.empty());
        for (String a : w.abilities) lore.add(Component.text("» " + a, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        m.lore(lore);
        m.setUnbreakable(true);
        m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        m.setCustomModelData(w.cmd);
        if (w == Weapon.CHASTIEFOL) { m.addEnchant(Enchantment.LOYALTY, 3, true); m.addItemFlags(ItemFlag.HIDE_ENCHANTS); }
        if (w == Weapon.HERRITT) { m.addEnchant(Enchantment.INFINITY, 1, true); m.addEnchant(Enchantment.POWER, 3, true); m.addItemFlags(ItemFlag.HIDE_ENCHANTS); }
        if (w == Weapon.COURECHOUSE) { // nunchaku : arme rapide custom (rien d'equivalent dans vanilla)
            m.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(new NamespacedKey(this, "cou_dmg"), 4.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            m.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(new NamespacedKey(this, "cou_spd"), 1.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        }
        m.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, w.id);
        it.setItemMeta(m);
        return it;
    }

    List<Component> wrap(String s, int width, TextColor c) {
        List<Component> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            if (line.length() + word.length() + 1 > width) {
                out.add(Component.text(line.toString(), c).decoration(TextDecoration.ITALIC, false));
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) out.add(Component.text(line.toString(), c).decoration(TextDecoration.ITALIC, false));
        return out;
    }

    Weapon weaponOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        String id = it.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        return id == null ? null : Weapon.byId(id);
    }

    boolean has(Player p, Weapon w) {
        for (ItemStack it : p.getInventory().getContents()) if (weaponOf(it) == w) return true;
        return false;
    }

    // ============ UTILITAIRES ============
    boolean cd(Player p, String key, int sec, boolean msg) {
        String k = p.getUniqueId() + key;
        long now = System.currentTimeMillis();
        Long end = cds.get(k);
        if (end != null && end > now) {
            if (msg) p.sendActionBar(Component.text("⏳ Recharge : " + ((end - now) / 1000 + 1) + "s", NamedTextColor.RED));
            return false;
        }
        cds.put(k, now + sec * 1000L);
        return true;
    }
    boolean cd(Player p, String key, int sec) { return cd(p, key, sec, true); }

    boolean valid(Player owner, Entity e) {
        return e instanceof LivingEntity && !(e instanceof ArmorStand) && !e.equals(owner) && !minions.contains(e.getUniqueId());
    }

    List<LivingEntity> along(Player p, double range, double radius) {
        List<LivingEntity> res = new ArrayList<>();
        Location eye = p.getEyeLocation();
        Vector d = eye.getDirection();
        for (Entity en : p.getNearbyEntities(range, range, range)) {
            if (!valid(p, en)) continue;
            LivingEntity le = (LivingEntity) en;
            Vector to = le.getEyeLocation().toVector().subtract(eye.toVector());
            double t = to.dot(d);
            if (t < 0 || t > range) continue;
            if (to.clone().subtract(d.clone().multiply(t)).length() <= radius) res.add(le);
        }
        res.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(p.getLocation())));
        return res;
    }

    double blockDist(Player p, double range) {
        RayTraceResult r = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), range);
        return r == null ? range : p.getEyeLocation().distance(r.getHitPosition().toLocation(p.getWorld()));
    }

    void beam(Player p, double len, Particle pt, Object data) {
        Location eye = p.getEyeLocation().add(0, -0.3, 0);
        Vector d = eye.getDirection();
        for (double i = 1; i < len; i += 0.4) {
            Location l = eye.clone().add(d.clone().multiply(i));
            if (data != null) p.getWorld().spawnParticle(pt, l, 1, 0, 0, 0, 0, data);
            else p.getWorld().spawnParticle(pt, l, 1, 0, 0, 0, 0);
        }
    }

    void effect(LivingEntity e, PotionEffectType t, int ticks, int amp) { e.addPotionEffect(new PotionEffect(t, ticks, amp)); }

    void later(Runnable r, int ticks) { Bukkit.getScheduler().runTaskLater(this, r, ticks); }

    Entity attacker(Entity d) {
        if (d instanceof Projectile pr && pr.getShooter() instanceof Entity s) return s;
        return d;
    }

    // ============ INTERACTIONS ============
    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        Weapon w = weaponOf(p.getInventory().getItemInMainHand());
        if (w == null) return;
        Action a = e.getAction();
        boolean right = a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
        boolean left = a == Action.LEFT_CLICK_AIR;

        if (w == Weapon.GIDEON && left) { gideonLightning(p); return; }
        if (w == Weapon.CHASTIEFOL) {
            if (right && p.isSneaking()) { e.setCancelled(true); chastiefol(p, true); }
            else if (left) chastiefol(p, false);
            return;
        }
        if (w == Weapon.HERRITT) { if (left && p.isSneaking()) herrittIllusion(p); return; }
        if (!right) return;
        e.setCancelled(true);

        switch (w) {
            case LOSTVAYNE -> lostvayne(p);
            case GIDEON -> gideon(p);
            case RHITTA -> rhitta(p);
                        case COURECHOUSE -> courechouse(p);
            case ALDAN -> aldan(p);
            default -> {}
        }
    }

    // ---- Lostvayne ----
    void lostvayne(Player p) {
        if (!cd(p, "lost", 30)) return;
        removeClones(p.getUniqueId());
        List<ArmorStand> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double ang = Math.PI * 2 * i / 4;
            Location l = p.getLocation().add(Math.cos(ang) * 2, 0, Math.sin(ang) * 2);
            l.setYaw(p.getLocation().getYaw());
            ArmorStand as = p.getWorld().spawn(l, ArmorStand.class, s -> {
                s.setArms(true);
                s.setBasePlate(false);
                s.customName(Component.text("Clone de " + p.getName(), NamedTextColor.GREEN));
                s.setCustomNameVisible(true);
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                sm.setOwningPlayer(p);
                head.setItemMeta(sm);
                s.getEquipment().setHelmet(head);
                ItemStack[] arm = p.getInventory().getArmorContents();
                s.getEquipment().setBoots(arm[0]);
                s.getEquipment().setLeggings(arm[1]);
                s.getEquipment().setChestplate(arm[2]);
                s.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            });
            list.add(as);
            cloneOwner.put(as.getUniqueId(), p.getUniqueId());
            p.getWorld().spawnParticle(Particle.CLOUD, l.add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
        }
        clones.put(p.getUniqueId(), list);
        p.sendMessage(Component.text("Jitsuzō Bunshin ! Ta force est divisée entre tes clones.", NamedTextColor.GREEN));
        UUID id = p.getUniqueId();
        later(() -> removeClones(id), 20 * 30);
    }

    void removeClones(UUID owner) {
        List<ArmorStand> l = clones.remove(owner);
        if (l == null) return;
        for (ArmorStand a : l) { cloneOwner.remove(a.getUniqueId()); if (a.isValid()) { a.getWorld().spawnParticle(Particle.CLOUD, a.getLocation().add(0, 1, 0), 10); a.remove(); } }
    }

    // ---- Chastiefol ----
    void chastiefol(Player p, boolean cycle) {
        UUID id = p.getUniqueId();
        int f = forms.getOrDefault(id, 0);
        if (cycle) {
            f = (f + 1) % FORMS.length;
            forms.put(id, f);
            p.sendActionBar(Component.text("Forme : " + FORMS[f], NamedTextColor.GOLD));
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1.5f);
            return;
        }
        Location loc = p.getLocation();
        switch (f) {
            case 0 -> { // Chastiefol : tir de lance
                if (!cd(p, "ch0", 3)) return;
                double len = blockDist(p, 25);
                beam(p, len, Particle.END_ROD, null);
                for (LivingEntity t : along(p, len, 1.2)) { t.damage(10, p); break; }
                p.playSound(loc, Sound.ITEM_TRIDENT_THROW, 1, 1);
            }
            case 1 -> { // Guardian
                if (!cd(p, "ch1", 40)) return;
                IronGolem g = p.getWorld().spawn(loc.clone().add(loc.getDirection().multiply(2)), IronGolem.class, s -> {
                    s.setPlayerCreated(true);
                    s.customName(Component.text("Guardian", NamedTextColor.DARK_GREEN));
                    s.setCustomNameVisible(true);
                    s.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 30, 0));
                });
                minions.add(g.getUniqueId());
                later(() -> { minions.remove(g.getUniqueId()); g.remove(); }, 20 * 30);
            }
            case 2 -> { // Fossilization
                if (!cd(p, "ch2", 20)) return;
                List<LivingEntity> ts = along(p, 8, 1.3);
                if (ts.isEmpty()) { cds.remove(p.getUniqueId() + "ch2"); p.sendActionBar(Component.text("Aucune cible", NamedTextColor.GRAY)); return; }
                LivingEntity t = ts.get(0);
                effect(t, PotionEffectType.SLOWNESS, 100, 9);
                effect(t, PotionEffectType.MINING_FATIGUE, 100, 4);
                effect(t, PotionEffectType.JUMP_BOOST, 100, 128);
                if (!(t instanceof Player)) { t.setAI(false); later(() -> t.setAI(true), 100); }
                t.getWorld().spawnParticle(Particle.BLOCK, t.getLocation().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0, Material.STONE.createBlockData());
                t.getWorld().playSound(t.getLocation(), Sound.BLOCK_STONE_PLACE, 1, 0.5f);
            }
            case 3 -> { // Sunflower
                if (!cd(p, "ch3", 45)) return;
                Location sl = loc.clone().add(loc.getDirection().setY(0).normalize().multiply(3));
                ArmorStand st = p.getWorld().spawn(sl, ArmorStand.class, s -> {
                    s.setInvisible(true); s.setMarker(true); s.setGravity(false);
                    s.getEquipment().setHelmet(new ItemStack(Material.SUNFLOWER));
                });
                new BukkitRunnable() {
                    int t = 0;
                    public void run() {
                        if (t++ >= 16 || !st.isValid()) { st.remove(); cancel(); return; }
                        for (Entity en : st.getNearbyEntities(10, 6, 10)) {
                            if (!valid(p, en)) continue;
                            LivingEntity le = (LivingEntity) en;
                            le.damage(3, p);
                            Vector d = le.getEyeLocation().toVector().subtract(st.getLocation().add(0, 1.5, 0).toVector());
                            double len = d.length(); d.normalize();
                            for (double i = 0; i < len; i += 0.5) st.getWorld().spawnParticle(Particle.FLAME, st.getLocation().add(0, 1.5, 0).add(d.clone().multiply(i)), 1, 0, 0, 0, 0);
                        }
                    }
                }.runTaskTimer(this, 0, 10);
            }
            case 4 -> { // Increase
                if (!cd(p, "ch4", 10)) return;
                for (int i = 0; i < 12; i++) {
                    Vector v = p.getEyeLocation().getDirection().add(new Vector((Math.random() - .5) * .35, (Math.random() - .5) * .35, (Math.random() - .5) * .35)).multiply(3);
                    Arrow ar = p.launchProjectile(Arrow.class, v);
                    ar.setDamage(3);
                    ar.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    later(ar::remove, 100);
                }
                p.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, 1, 1.4f);
            }
            case 5 -> { // Yggdra Cloth
                if (!cd(p, "ch5", 60)) return;
                effect(p, PotionEffectType.RESISTANCE, 20 * 30, 1);
                effect(p, PotionEffectType.STRENGTH, 20 * 30, 1);
                effect(p, PotionEffectType.SPEED, 20 * 30, 0);
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.add(0, 1, 0), 40, 0.5, 0.8, 0.5);
            }
            case 6 -> { // Luminosity
                if (!cd(p, "ch6", 30)) return;
                effect(p, PotionEffectType.NIGHT_VISION, 20 * 60, 0);
                for (Entity en : p.getNearbyEntities(20, 20, 20)) if (valid(p, en)) effect((LivingEntity) en, PotionEffectType.GLOWING, 20 * 10, 0);
                p.getWorld().spawnParticle(Particle.END_ROD, loc.add(0, 1.5, 0), 60, 1, 1, 1, 0.05);
            }
            case 7 -> { // Pollen Garden
                if (!cd(p, "ch7", 40)) return;
                Location c = loc.clone();
                new BukkitRunnable() {
                    int t = 0;
                    public void run() {
                        if (t++ >= 10) { cancel(); return; }
                        c.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, c.clone().add(0, 1, 0), 40, 3, 1, 3);
                        for (Entity en : c.getWorld().getNearbyEntities(c, 6, 4, 6))
                            if (en instanceof Player pl) { effect(pl, PotionEffectType.REGENERATION, 60, 1); effect(pl, PotionEffectType.RESISTANCE, 60, 0); }
                    }
                }.runTaskTimer(this, 0, 20);
            }
        }
    }

    // ---- Gideon ----
    void gideon(Player p) {
        Location loc = p.getLocation();
        if (p.isSneaking()) {
            if (!cd(p, "gid2", 60)) return;
            IronGolem g = p.getWorld().spawn(loc.clone().add(loc.getDirection().multiply(2)), IronGolem.class, s -> {
                s.setPlayerCreated(true);
                s.customName(Component.text("Golem de terre", NamedTextColor.GOLD));
                s.setCustomNameVisible(true);
            });
            minions.add(g.getUniqueId());
            later(() -> { minions.remove(g.getUniqueId()); g.remove(); }, 20 * 30);
            return;
        }
        if (!cd(p, "gid1", 20)) return;
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.5f);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 150, 4, 0.3, 4, 0, Material.DIRT.createBlockData());
        for (Entity en : p.getNearbyEntities(8, 4, 8)) {
            if (!valid(p, en)) continue;
            LivingEntity le = (LivingEntity) en;
            le.damage(8, p);
            le.setVelocity(new Vector(0, 1.0, 0));
        }
    }

    void gideonLightning(Player p) {
        if (!cd(p, "gid3", 10)) return;
        List<LivingEntity> ts = along(p, 30, 1.5);
        Location target;
        if (!ts.isEmpty()) target = ts.get(0).getLocation();
        else {
            RayTraceResult r = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 30);
            if (r == null) { cds.remove(p.getUniqueId() + "gid3"); return; }
            target = r.getHitPosition().toLocation(p.getWorld());
        }
        p.getWorld().strikeLightning(target);
    }

    // ---- Rhitta ----
    void rhitta(Player p) {
        int c = charge.getOrDefault(p.getUniqueId(), 0);
        if (c < 10) { p.sendActionBar(Component.text("Pas assez de chaleur solaire…", NamedTextColor.RED)); return; }
        if (!cd(p, "rhi", 5)) return;
        double len = blockDist(p, 8);
        Location t = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(Math.max(2, len)));
        float power = 2f + 6f * c / 100f;
        boolean blocks = getConfig().getBoolean("rhitta-block-damage", false);
        t.getWorld().createExplosion(t, power, true, blocks, p);
        t.getWorld().spawnParticle(Particle.FLAME, t, 200, power / 2, power / 2, power / 2, 0.1);
        for (Entity en : t.getWorld().getNearbyEntities(t, power, power, power)) if (valid(p, en)) en.setFireTicks(20 * 5);
        charge.put(p.getUniqueId(), 0);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (weaponOf(e.getItemDrop().getItemStack()) == Weapon.RHITTA) rhittaDrop.put(e.getPlayer().getUniqueId(), e.getItemDrop().getUniqueId());
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        UUID id = rhittaDrop.get(p.getUniqueId());
        if (id == null) return;
        Entity en = Bukkit.getEntity(id);
        if (en instanceof Item item && item.isValid()) {
            e.setCancelled(true);
            item.teleport(p.getLocation());
            item.setPickupDelay(0);
            p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
            p.sendActionBar(Component.text("Rhitta revient vers toi !", NamedTextColor.YELLOW));
        }
        rhittaDrop.remove(p.getUniqueId());
    }

    // ---- Herritt (arc vanilla : 2 fleches de lumiere) ----
    void herrittIllusion(Player p) {
        if (!cd(p, "her2", 40)) return;
        Particle.DustOptions dust = new Particle.DustOptions(Color.FUCHSIA, 1.6f);
        for (Entity en : p.getNearbyEntities(30, 30, 30)) {
            if (!valid(p, en)) continue;
            LivingEntity le = (LivingEntity) en;
            effect(le, PotionEffectType.NAUSEA, 20 * 15, 0);
            effect(le, PotionEffectType.DARKNESS, 20 * 15, 0);
            effect(le, PotionEffectType.BLINDNESS, 20 * 4, 0);
            if (le instanceof Player v) v.sendMessage(Component.text("Tu es piégé dans une illusion éternelle…", NamedTextColor.LIGHT_PURPLE));
        }
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 150, 5, 2, 5, 0, dust);
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p) || weaponOf(e.getBow()) != Weapon.HERRITT) return;
        if (!(e.getProjectile() instanceof AbstractArrow first)) return;
        // deuxieme arc : 2eme fleche legerement decalee
        Vector v = first.getVelocity();
        Vector side = v.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(v.length() * 0.06);
        first.setVelocity(v.clone().add(side));
        Arrow second = p.launchProjectile(Arrow.class, v.clone().subtract(side));
        second.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        second.setDamage(first.getDamage());
        second.setCritical(first.isCritical());
        for (AbstractArrow ar : new AbstractArrow[]{first, second}) {
            ar.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            ar.setGlowing(true);
            ar.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, "herritt");
            new BukkitRunnable() {
                int t = 0;
                public void run() {
                    if (!ar.isValid() || ar.isInBlock() || t++ > 100) { if (ar.isValid()) ar.remove(); cancel(); return; }
                    ar.getWorld().spawnParticle(Particle.DUST, ar.getLocation(), 3, 0.05, 0.05, 0.05, 0, new Particle.DustOptions(Color.FUCHSIA, 1.4f));
                }
            }.runTaskTimer(this, 0, 1);
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent e) {
        if (!"herritt".equals(e.getEntity().getPersistentDataContainer().get(KEY, PersistentDataType.STRING))) return;
        if (e.getHitEntity() instanceof LivingEntity le) {
            effect(le, PotionEffectType.NAUSEA, 20 * 8, 0);
            effect(le, PotionEffectType.DARKNESS, 20 * 8, 0);
            effect(le, PotionEffectType.SLOWNESS, 20 * 3, 1);
            if (le instanceof Player v) v.sendMessage(Component.text("Tes souvenirs vacillent…", NamedTextColor.LIGHT_PURPLE));
        }
        e.getEntity().remove();
    }

    // ---- Courechouse ----
    void courechouse(Player p) {
        if (!cd(p, "cou", 60)) return;
        effect(p, PotionEffectType.SPEED, 20 * 15, 2);
        effect(p, PotionEffectType.STRENGTH, 20 * 15, 1);
        effect(p, PotionEffectType.JUMP_BOOST, 20 * 15, 1);
        int n = 0;
        for (Entity en : p.getNearbyEntities(15, 15, 15)) {
            if (!valid(p, en)) continue;
            LivingEntity le = (LivingEntity) en;
            effect(le, PotionEffectType.WEAKNESS, 20 * 10, 1);
            effect(le, PotionEffectType.SLOWNESS, 20 * 10, 1);
            n++;
        }
        p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p.getLocation().add(0, 1, 0), 80, 1, 1, 1, 0.05);
        p.sendMessage(Component.text("Super Concentration ! Tu as volé la force de " + n + " ennemi(s).", NamedTextColor.RED));
    }

    // ---- Aldan ----
    void aldan(Player p) {
        if (p.isSneaking()) {
            if (!cd(p, "ald2", 15)) return;
            double len = blockDist(p, 60);
            beam(p, len, Particle.WITCH, null);
            Location t = p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(len));
            t.getWorld().createExplosion(t, 3f, false, false, p);
            for (Entity en : t.getWorld().getNearbyEntities(t, 4, 4, 4)) if (valid(p, en)) ((LivingEntity) en).damage(10, p);
            return;
        }
        if (!cd(p, "ald1", 30)) return;
        int n = 0;
        for (Entity en : p.getNearbyEntities(64, 64, 64)) {
            if (!valid(p, en)) continue;
            effect((LivingEntity) en, PotionEffectType.GLOWING, 20 * 10, 0);
            n++;
        }
        effect(p, PotionEffectType.NIGHT_VISION, 20 * 10, 0);
        p.sendMessage(Component.text("Clairvoyance : " + n + " présence(s) détectée(s) alentour.", NamedTextColor.AQUA));
    }

    // ============ DEGATS ============
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Weapon held = weaponOf(p.getInventory().getItemInMainHand());
        // Gideon : foudre + Lightning Disruption
        if (held == Weapon.GIDEON) {
            EntityDamageEvent.DamageCause c = e.getCause();
            boolean magic = c == EntityDamageEvent.DamageCause.MAGIC || c == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                    || c == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || c == EntityDamageEvent.DamageCause.DRAGON_BREATH;
            if (c == EntityDamageEvent.DamageCause.LIGHTNING || (p.isSneaking() && magic)) {
                e.setCancelled(true);
                p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p.getLocation(), 40, 0.5, 0.2, 0.5, 0.1);
                return;
            }
        }
        // Aldan : sauvegarde de l'âme
        if (p.getHealth() - e.getFinalDamage() <= 0 && has(p, Weapon.ALDAN) && cd(p, "ald3", 300, false)) {
            e.setCancelled(true);
            p.setHealth(Math.min(10, p.getMaxHealth()));
            p.setFireTicks(0);
            effect(p, PotionEffectType.RESISTANCE, 20 * 5, 4);
            p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1, 0), 120, 0.5, 1, 0.5, 1);
            p.sendMessage(Component.text("Aldan sauvegarde ton âme !", NamedTextColor.AQUA));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (busy) return;
        Entity dmgr = attacker(e.getDamager());
        // Clone touché : Contre Total
        UUID ownerId = cloneOwner.get(e.getEntity().getUniqueId());
        if (ownerId != null) {
            e.setCancelled(true);
            if (dmgr instanceof LivingEntity le && !le.getUniqueId().equals(ownerId)) reflect(le, e.getDamage() * 2, Bukkit.getPlayer(ownerId));
            return;
        }
        // Meliodas : Contre Total en sneak / force divisée
        if (e.getEntity() instanceof Player v && weaponOf(v.getInventory().getItemInMainHand()) == Weapon.LOSTVAYNE && v.isSneaking()
                && dmgr instanceof LivingEntity le) {
            e.setCancelled(true);
            reflect(le, e.getDamage() * 2, v);
            return;
        }
        if (dmgr instanceof Player p && e.getDamager() == p) {
            Weapon w = weaponOf(p.getInventory().getItemInMainHand());
            if (w == Weapon.LOSTVAYNE) {
                List<ArmorStand> l = clones.get(p.getUniqueId());
                if (l != null && !l.isEmpty()) e.setDamage(e.getDamage() / (l.size() + 1));
            } else if (w == Weapon.COURECHOUSE && e.getEntity() instanceof LivingEntity t) {
                long now = System.currentTimeMillis();
                long[] cb = combo.computeIfAbsent(p.getUniqueId(), k -> new long[]{0, 0});
                cb[0] = (now - cb[1] < 1500) ? Math.min(cb[0] + 1, 5) : 1;
                cb[1] = now;
                e.setDamage(e.getDamage() * (1 + 0.1 * cb[0]));
                p.sendActionBar(Component.text("Combo x" + cb[0], NamedTextColor.RED));
                effect(t, PotionEffectType.WEAKNESS, 60, 0);
                effect(t, PotionEffectType.SLOWNESS, 60, 0);
                effect(p, PotionEffectType.STRENGTH, 60, 0);
                effect(p, PotionEffectType.SPEED, 60, 0);
            }
        }
    }

    void reflect(LivingEntity target, double dmg, Player source) {
        busy = true;
        try {
            if (source != null) target.damage(dmg, source); else target.damage(dmg);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.2);
        } finally { busy = false; }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { removeClones(e.getPlayer().getUniqueId()); }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) { removeClones(e.getEntity().getUniqueId()); }

    // ============ COMMANDE ============
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!s.hasPermission("sevensins.admin")) { s.sendMessage(Component.text("Permission refusée.", NamedTextColor.RED)); return true; }
        if (a.length == 0 || a[0].equalsIgnoreCase("list")) {
            s.sendMessage(Component.text("Trésors : lostvayne, chastiefol, gideon, rhitta, herritt, courechouse, aldan", NamedTextColor.GOLD));
            s.sendMessage(Component.text("/sin give <arme> [joueur]  |  /sin giveall [joueur]", NamedTextColor.GRAY));
            return true;
        }
        Player target = null;
        if (a[0].equalsIgnoreCase("give") && a.length >= 2) {
            Weapon w = Weapon.byId(a[1]);
            if (w == null) { s.sendMessage(Component.text("Arme inconnue.", NamedTextColor.RED)); return true; }
            target = a.length >= 3 ? Bukkit.getPlayer(a[2]) : (s instanceof Player p ? p : null);
            if (target == null) { s.sendMessage(Component.text("Joueur introuvable.", NamedTextColor.RED)); return true; }
            target.getInventory().addItem(create(w));
            s.sendMessage(Component.text(w.display + " donné à " + target.getName(), NamedTextColor.GREEN));
        } else if (a[0].equalsIgnoreCase("giveall")) {
            target = a.length >= 2 ? Bukkit.getPlayer(a[1]) : (s instanceof Player p ? p : null);
            if (target == null) { s.sendMessage(Component.text("Joueur introuvable.", NamedTextColor.RED)); return true; }
            for (Weapon w : Weapon.values()) target.getInventory().addItem(create(w));
            s.sendMessage(Component.text("Les 7 trésors ont été donnés à " + target.getName(), NamedTextColor.GREEN));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) out.addAll(List.of("give", "giveall", "list"));
        else if (a.length == 2 && a[0].equalsIgnoreCase("give")) for (Weapon w : Weapon.values()) out.add(w.id);
        else for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        String last = a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.toLowerCase().startsWith(last));
        return out;
    }
}
