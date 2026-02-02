package com.nageoffer.shortlink.admin;

public class UserTableShardingText {

    static final String SQL = "ALTER TABLE t_link_%d ADD UNIQUE INDEX idx_unique_full_short_url (full_short_url, del_time);";
    public static void main(String[] args) {
        for (int i = 0;i < 16;i ++)
        {
            System.out.printf((SQL) + "%n",i);
        }

    }
}
